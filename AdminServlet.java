package in.securebank.web;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;

/** Server-side role check for administrator-only customer/KYC overview. */
@WebServlet("/api/admin/customers")
public class AdminServlet extends HttpServlet {
  protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws IOException{
    HttpSession s=req.getSession(false);
    if(s==null||!"ADMIN".equals(s.getAttribute("role"))){resp.sendError(403,"Administrator access required");return;}
    String url=System.getenv().getOrDefault("SECUREBANK_DB_URL","jdbc:mysql://localhost:3306/securebank?useSSL=false&serverTimezone=UTC");
    String user=System.getenv().getOrDefault("SECUREBANK_DB_USER","root"),pass=System.getenv().getOrDefault("SECUREBANK_DB_PASSWORD","");
    resp.setContentType("application/json");resp.setCharacterEncoding("UTF-8");
    try(Connection c=DriverManager.getConnection(url,user,pass);PreparedStatement q=c.prepareStatement("SELECT id,full_name,email,kyc_status,created_at FROM customers ORDER BY created_at DESC LIMIT 200");ResultSet r=q.executeQuery()){
      var out=resp.getWriter();out.write("{\"customers\":[");boolean first=true;while(r.next()){if(!first)out.write(",");first=false;out.write("{\"id\":"+r.getLong("id")+",\"name\":\""+esc(r.getString("full_name"))+"\",\"email\":\""+esc(r.getString("email"))+"\",\"kycStatus\":\""+esc(r.getString("kyc_status"))+"\",\"createdAt\":\""+esc(r.getString("created_at"))+"\"}");}out.write("]}");
    }catch(SQLException e){getServletContext().log("Admin customer list failed",e);resp.setStatus(500);resp.getWriter().write("{\"error\":\"Database unavailable\"}");}
  }
  private String esc(String s){return s==null?"":s.replace("\\","\\\\").replace("\"","\\\"").replace("\n"," ").replace("\r"," ");}
}
