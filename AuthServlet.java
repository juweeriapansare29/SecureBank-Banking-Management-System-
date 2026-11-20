package in.securebank.web;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@WebServlet("/api/auth")
public class AuthServlet extends HttpServlet {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long CODE_LIFETIME_MS = 5 * 60 * 1000L;
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_MS = 15 * 60 * 1000L;
    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();

    private static class AttemptState { int failures; long lockedUntil; }
    private String env(String key, String fallback) { return System.getenv().getOrDefault(key, fallback); }
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            env("SECUREBANK_DB_URL","jdbc:mysql://localhost:3306/securebank?useSSL=false&serverTimezone=UTC"),
            env("SECUREBANK_DB_USER","root"), env("SECUREBANK_DB_PASSWORD",""));
    }
    private void json(HttpServletResponse r,int status,String body)throws IOException {
        r.setStatus(status); r.setContentType("application/json"); r.setCharacterEncoding("UTF-8"); r.getWriter().write(body);
    }
    private String clean(String s){return s==null?"":s.trim();}
    private String esc(String s){return s==null?"":s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r").replace("\t","\\t");}
    private String hashPassword(String password)throws Exception {
        byte[] salt=new byte[16]; RANDOM.nextBytes(salt); int iterations=210000;
        PBEKeySpec spec=new PBEKeySpec(password.toCharArray(),salt,iterations,256);
        try { byte[] hash=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return "pbkdf2$"+iterations+"$"+Base64.getEncoder().encodeToString(salt)+"$"+Base64.getEncoder().encodeToString(hash);
        } finally {spec.clearPassword();}
    }
    private boolean verifyPassword(String password,String stored)throws Exception {
        String[] p=stored.split("\\$"); if(p.length!=4||!"pbkdf2".equals(p[0]))return false;
        int it=Integer.parseInt(p[1]); if(it<100000||it>1000000)return false;
        byte[] salt=Base64.getDecoder().decode(p[2]), expected=Base64.getDecoder().decode(p[3]);
        PBEKeySpec spec=new PBEKeySpec(password.toCharArray(),salt,it,expected.length*8);
        try {return MessageDigest.isEqual(expected,SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded());}
        finally {spec.clearPassword();}
    }
    private String code(){return String.format(Locale.ROOT,"%06d",RANDOM.nextInt(1_000_000));}
    private String digest(String s)throws Exception {return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}
    private void sendEmail(String to,String subject,String body)throws Exception {
        String host=env("SECUREBANK_SMTP_HOST","smtp.gmail.com");
        String port=env("SECUREBANK_SMTP_PORT","587");
        String user=env("SECUREBANK_SMTP_USER","");
        String pass=env("SECUREBANK_SMTP_PASSWORD","");
        String from=env("SECUREBANK_SMTP_FROM",user);
        if(user.isBlank()||pass.isBlank()||from.isBlank())throw new IllegalStateException("Email is not configured. Set SECUREBANK_SMTP_USER, SECUREBANK_SMTP_PASSWORD, and SECUREBANK_SMTP_FROM.");
        Properties props=new Properties();
        props.put("mail.smtp.auth","true"); props.put("mail.smtp.starttls.enable","true");
        props.put("mail.smtp.host",host); props.put("mail.smtp.port",port);
        props.put("mail.smtp.ssl.protocols","TLSv1.2 TLSv1.3");
        Session mailSession=Session.getInstance(props,new Authenticator(){protected PasswordAuthentication getPasswordAuthentication(){return new PasswordAuthentication(user,pass);}});
        MimeMessage msg=new MimeMessage(mailSession); msg.setFrom(new InternetAddress(from)); msg.setRecipients(Message.RecipientType.TO,InternetAddress.parse(to,false));
        msg.setSubject(subject,"UTF-8"); msg.setText(body,"UTF-8"); Transport.send(msg);
    }
    private void startCode(HttpSession s,String type,String email,String code){
        try{s.setAttribute(type+"Email",email);s.setAttribute(type+"Digest",digest(code));s.setAttribute(type+"Expires",System.currentTimeMillis()+CODE_LIFETIME_MS);s.setAttribute(type+"Tries",0);}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    private boolean verifyCode(HttpSession s,String type,String candidate)throws Exception {
        Long expires=(Long)s.getAttribute(type+"Expires"); String expected=(String)s.getAttribute(type+"Digest");
        Integer tries=(Integer)s.getAttribute(type+"Tries");
        if(expires==null||expected==null||System.currentTimeMillis()>expires||(tries!=null&&tries>=5))return false;
        s.setAttribute(type+"Tries",(tries==null?0:tries)+1);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),digest(clean(candidate)).getBytes(StandardCharsets.UTF_8));
    }
    private boolean locked(String email){AttemptState s=attempts.get(email);return s!=null&&s.lockedUntil>System.currentTimeMillis();}
    private void failed(String email){AttemptState s=attempts.computeIfAbsent(email,k->new AttemptState());synchronized(s){s.failures++;if(s.failures>=MAX_ATTEMPTS){s.lockedUntil=System.currentTimeMillis()+LOCK_MS;s.failures=0;}}}
    private void succeeded(String email){attempts.remove(email);}
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws IOException {
        HttpSession s=req.getSession(false);
        if(s==null||s.getAttribute("customerId")==null){json(res,401,"{\"error\":\"Not authenticated\"}");return;}
        json(res,200,"{\"id\":"+s.getAttribute("customerId")+",\"name\":\""+esc((String)s.getAttribute("customerName"))+"\",\"role\":\""+esc((String)s.getAttribute("role"))+"\"}");
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException {
        req.setCharacterEncoding("UTF-8"); String action=clean(req.getParameter("action"));
        String email=clean(req.getParameter("email")).toLowerCase(Locale.ROOT); String password=req.getParameter("password");
        try {
            if("logout".equals(action)){HttpSession s=req.getSession(false);if(s!=null)s.invalidate();json(res,200,"{\"ok\":true}");return;}
            if("verify-otp".equals(action)){verifyOtp(req,res);return;}
            if("resend-otp".equals(action)){resendOtp(req,res);return;}
            if("forgot-password".equals(action)){forgotPassword(req,res,email);return;}
            if("reset-password".equals(action)){resetPassword(req,res,email,password);return;}
            if(!"login".equals(action)&&!"register".equals(action)){json(res,400,"{\"error\":\"Unknown action.\"}");return;}
            if(email.length()>190||!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")||password==null||password.length()<10||password.length()>128){json(res,400,"{\"error\":\"Use a valid email and a password of 10–128 characters.\"}");return;}
            if("register".equals(action))register(req,res,email,password);else login(req,res,email,password);
        } catch(Exception ex){getServletContext().log("SecureBank auth error",ex);String m=ex.getMessage()!=null&&ex.getMessage().startsWith("Email is not configured")?ex.getMessage():"Authentication service unavailable. Check server logs and configuration.";json(res,500,"{\"error\":\""+esc(m)+"\"}");}
    }
    private void register(HttpServletRequest req,HttpServletResponse res,String email,String password)throws Exception {
        String name=clean(req.getParameter("name"));if(name.length()<2||name.length()>100){json(res,400,"{\"error\":\"Enter your full name.\"}");return;}
        long id;
        try(Connection c=getConnection()){
            c.setAutoCommit(false);
            try {
                try(PreparedStatement p=c.prepareStatement("INSERT INTO customers (full_name,email,password_hash,role,kyc_status) VALUES (?,?,?,'CUSTOMER','NOT_SUBMITTED')",Statement.RETURN_GENERATED_KEYS)){
                    p.setString(1,name);p.setString(2,email);p.setString(3,hashPassword(password));p.executeUpdate();try(ResultSet rs=p.getGeneratedKeys()){if(!rs.next())throw new SQLException("Missing customer ID");id=rs.getLong(1);}
                }
                try(PreparedStatement p=c.prepareStatement("INSERT INTO bank_accounts (customer_id,account_number,owner_name,account_type,balance,status) VALUES (?,?,?,'SAVINGS',10000.00,'ACTIVE')")){
                    p.setLong(1,id);p.setString(2,"SB"+String.format("%010d",id));p.setString(3,name);p.executeUpdate();
                } c.commit();
            }catch(Exception e){c.rollback();if(e instanceof SQLException se&&se.getErrorCode()==1062){json(res,409,"{\"error\":\"An account with this email already exists.\"}");return;}throw e;}
        }
        String otp=code(); HttpSession s=req.getSession(true);req.changeSessionId();
        s.setAttribute("pendingCustomerId",id);s.setAttribute("pendingCustomerName",name);s.setAttribute("pendingRole","CUSTOMER");s.setAttribute("pendingEmail",email);startCode(s,"loginOtp",email,otp);
        sendEmail(email,"SecureBank sign-in verification","Your SecureBank verification code is "+otp+". It expires in 5 minutes. If you did not request this, ignore this email.");
        json(res,201,"{\"ok\":true,\"requiresOtp\":true}");
    }
    private void login(HttpServletRequest req,HttpServletResponse res,String email,String password)throws Exception {
        if(locked(email)){json(res,429,"{\"error\":\"Too many failed attempts. Try again in 15 minutes.\"}");return;}
        try(Connection c=getConnection();PreparedStatement p=c.prepareStatement("SELECT id,full_name,password_hash,role FROM customers WHERE email=? AND status='ACTIVE'")){
            p.setString(1,email);try(ResultSet rs=p.executeQuery()){
                if(!rs.next()||!verifyPassword(password,rs.getString("password_hash"))){failed(email);json(res,401,"{\"error\":\"Email or password is incorrect.\"}");return;}
                long id=rs.getLong("id");String name=rs.getString("full_name"),role=rs.getString("role");succeeded(email);
                String otp=code();HttpSession old=req.getSession(false);if(old!=null)old.invalidate();HttpSession s=req.getSession(true);req.changeSessionId();
                s.setAttribute("pendingCustomerId",id);s.setAttribute("pendingCustomerName",name);s.setAttribute("pendingRole",role);s.setAttribute("pendingEmail",email);startCode(s,"loginOtp",email,otp);
                sendEmail(email,"SecureBank sign-in verification","Your SecureBank verification code is "+otp+". It expires in 5 minutes. Never share this code.");
                json(res,200,"{\"ok\":true,\"requiresOtp\":true}");
            }
        }
    }
    private void verifyOtp(HttpServletRequest req,HttpServletResponse res)throws Exception {
        HttpSession s=req.getSession(false);if(s==null||s.getAttribute("pendingCustomerId")==null){json(res,401,"{\"error\":\"Start sign-in again.\"}");return;}
        if(!verifyCode(s,"loginOtp",req.getParameter("code"))){json(res,401,"{\"error\":\"Invalid or expired code. Request a new code or sign in again.\"}");return;}
        s.removeAttribute("loginOtpDigest");s.removeAttribute("loginOtpExpires");s.removeAttribute("loginOtpTries");
        s.setAttribute("customerId",s.getAttribute("pendingCustomerId"));s.setAttribute("customerName",s.getAttribute("pendingCustomerName"));s.setAttribute("role",s.getAttribute("pendingRole"));
        String name=(String)s.getAttribute("customerName"),email=(String)s.getAttribute("pendingEmail"),role=(String)s.getAttribute("role");s.removeAttribute("pendingCustomerId");s.removeAttribute("pendingCustomerName");s.removeAttribute("pendingRole");s.removeAttribute("pendingEmail");
        res.setHeader("Cache-Control","no-store");json(res,200,"{\"ok\":true,\"name\":\""+esc(name)+"\",\"email\":\""+esc(email)+"\",\"role\":\""+esc(role)+"\"}");
    }
    private void resendOtp(HttpServletRequest req,HttpServletResponse res)throws Exception {
        HttpSession s=req.getSession(false);if(s==null||s.getAttribute("pendingCustomerId")==null){json(res,401,"{\"error\":\"Start sign-in again.\"}");return;}
        String email=(String)s.getAttribute("pendingEmail"),otp=code();startCode(s,"loginOtp",email,otp);sendEmail(email,"SecureBank sign-in verification","Your new SecureBank verification code is "+otp+". It expires in 5 minutes.");json(res,200,"{\"ok\":true}");
    }
    private void forgotPassword(HttpServletRequest req,HttpServletResponse res,String email)throws Exception {
        if(email.isBlank()||email.length()>190){json(res,400,"{\"error\":\"Enter your account email first.\"}");return;}
        boolean exists=false;try(Connection c=getConnection();PreparedStatement p=c.prepareStatement("SELECT id FROM customers WHERE email=? AND status='ACTIVE'")){p.setString(1,email);try(ResultSet rs=p.executeQuery()){exists=rs.next();}}
        if(exists){String otp=code();HttpSession s=req.getSession(true);s.setAttribute("resetEmail",email);startCode(s,"resetOtp",email,otp);sendEmail(email,"SecureBank password reset","Your SecureBank password reset code is "+otp+". It expires in 5 minutes. If you did not request this, ignore this email.");}
        json(res,200,"{\"ok\":true,\"message\":\"If an active account matches that email, a reset code has been sent.\"}");
    }
    private void resetPassword(HttpServletRequest req,HttpServletResponse res,String email,String password)throws Exception {
        HttpSession s=req.getSession(false);if(password==null||password.length()<10||password.length()>128){json(res,400,"{\"error\":\"New password must be 10–128 characters.\"}");return;}
        if(s==null||!email.equals(s.getAttribute("resetEmail"))||!email.equals(s.getAttribute("resetOtpEmail"))||!verifyCode(s,"resetOtp",req.getParameter("code"))){json(res,400,"{\"error\":\"Invalid or expired reset code. Request a new one.\"}");return;}
        try(Connection c=getConnection();PreparedStatement p=c.prepareStatement("UPDATE customers SET password_hash=? WHERE email=? AND status='ACTIVE'")){p.setString(1,hashPassword(password));p.setString(2,email);int n=p.executeUpdate();if(n!=1){json(res,400,"{\"error\":\"Unable to reset this account.\"}");return;}}
        s.removeAttribute("resetEmail");s.removeAttribute("resetOtpDigest");s.removeAttribute("resetOtpExpires");s.removeAttribute("resetOtpTries");json(res,200,"{\"ok\":true}");
    }
}
