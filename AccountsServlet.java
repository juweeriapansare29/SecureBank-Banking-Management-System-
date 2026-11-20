
package in.securebank.web;

import in.securebank.model.Account;
import in.securebank.model.CurrentAccount;
import in.securebank.model.SavingsAccount;
import in.securebank.persistence.HibernateUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.IOException;
import java.util.List;

@WebServlet("/api/accounts")
public class AccountsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            List<Account> accounts =
                    session.createQuery("from Account", Account.class).list();

            StringBuilder json = new StringBuilder("[");

            for (int i = 0; i < accounts.size(); i++) {
                Account account = accounts.get(i);

                if (i > 0) {
                    json.append(",");
                }

                json.append("{")
                    .append("\"accountNumber\":\"")
                    .append(escape(account.getAccountNumber()))
                    .append("\",")
                    .append("\"ownerName\":\"")
                    .append(escape(account.getOwnerName()))
                    .append("\",")
                    .append("\"accountType\":\"")
                    .append(escape(account.getAccountType()))
                    .append("\",")
                    .append("\"balance\":")
                    .append(account.getBalance())
                    .append("}");
            }

            json.append("]");
            response.getWriter().write(json.toString());

        } catch (Exception ex) {
            getServletContext().log("Failed to load SecureBank accounts", ex);
            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(
                    "{\"error\":\"Unable to load demo accounts\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String number = request.getParameter("accountNumber");
        String owner = request.getParameter("ownerName");
        String type = request.getParameter("type");

        if (number == null || owner == null || type == null
                || number.isBlank() || owner.isBlank()
                || type.isBlank()) {

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(
                    "{\"error\":\"accountNumber, ownerName and type are required\"}");
            return;
        }

        Account account;

        if ("SAVINGS".equalsIgnoreCase(type)) {
            account = new SavingsAccount(
                    number.trim(), owner.trim(), 0);

        } else if ("CURRENT".equalsIgnoreCase(type)) {
            account = new CurrentAccount(
                    number.trim(), owner.trim(), 0, 5000);

        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(
                    "{\"error\":\"type must be SAVINGS or CURRENT\"}");
            return;
        }

        Transaction tx = null;

        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            tx = session.beginTransaction();
            session.persist(account);
            tx.commit();

            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write(
                    "{\"message\":\"Demo account created\","
                    + "\"accountNumber\":\""
                    + escape(account.getAccountNumber()) + "\"}");

        } catch (Exception ex) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }

            getServletContext().log(
                    "Failed to create SecureBank account", ex);
            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(
                    "{\"error\":\"Could not create demo account; check server log\"}");
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}
