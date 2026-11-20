package in.securebank.persistence;

import in.securebank.model.*;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/** Hibernate bootstrap with credentials supplied via environment variables. */
public final class HibernateUtil {
    private static final SessionFactory SESSION_FACTORY = buildSessionFactory();

    private HibernateUtil() { }

    private static SessionFactory buildSessionFactory() {
        String url = requiredEnv("SECUREBANK_DB_URL");
        String user = requiredEnv("SECUREBANK_DB_USER");
        String password = requiredEnv("SECUREBANK_DB_PASSWORD");

        Configuration config = new Configuration();
        config.setProperty("hibernate.connection.driver_class", "com.mysql.cj.jdbc.Driver");
        config.setProperty("hibernate.connection.url", url);
        config.setProperty("hibernate.connection.username", user);
        config.setProperty("hibernate.connection.password", password);
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        config.setProperty("hibernate.hbm2ddl.auto", "validate");
        config.setProperty("hibernate.show_sql", "false");
        config.addAnnotatedClass(Account.class);
        config.addAnnotatedClass(SavingsAccount.class);
        config.addAnnotatedClass(CurrentAccount.class);
        config.addAnnotatedClass(Transaction.class);
        config.addAnnotatedClass(DepositTransaction.class);
        config.addAnnotatedClass(TransferTransaction.class);
        return config.buildSessionFactory();
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value;
    }

    public static SessionFactory getSessionFactory() { return SESSION_FACTORY; }
    public static void shutdown() {
        if (!SESSION_FACTORY.isClosed()) SESSION_FACTORY.close();
    }
}
