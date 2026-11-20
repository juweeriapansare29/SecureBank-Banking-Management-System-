# SecureBank — Java OOP Online Banking Management System

**Main syllabus topics:** Inheritance and Polymorphism  
**Supporting concepts:** Interfaces, exception handling, Collections  
**Stack:** Java 17, Jakarta Servlet, Hibernate ORM, MySQL, Maven

## What is included
- Abstract `Account` parent class with `SavingsAccount` and `CurrentAccount` subclasses
- Runtime polymorphism through overridden withdrawal and account-description methods
- `Transferable` interface
- Custom exceptions for invalid amounts and insufficient funds
- `List`, `Map` and `Set` collections in the in-memory service
- Hibernate entities for accounts and transactions
- Servlet endpoint for a sample account API
- MySQL schema and Hibernate configuration
- A console demo that can be run without a database
- A frontend placeholder page explaining the app/API setup

## Important distinction
This is an **academic starter project**, not a production-ready banking application. The console demo works independently. The Servlet/Hibernate/MySQL path requires the environment setup below and is intentionally kept minimal. It does not connect to any real bank or payment network. Never use real account, card, KYC or password information.

## 1. Run the OOP demo first
Install JDK 17+ and Maven. From this project directory run:

```bash
mvn -q compile exec:java -Dexec.mainClass=in.securebank.demo.SecureBankDemo
```

This shows inheritance, method overriding, runtime polymorphism, interface usage, custom exceptions and collections. Hibernate annotations are included in the model classes, so Maven resolves the required dependencies.

## 2. Configure MySQL for the web module
1. Create a database using `database/schema.sql`.
2. Set environment variables rather than committing credentials:
   - `SECUREBANK_DB_URL` (example: `jdbc:mysql://localhost:3306/securebank?serverTimezone=UTC`)
   - `SECUREBANK_DB_USER`
   - `SECUREBANK_DB_PASSWORD`
3. Configure a Jakarta Servlet 6 compatible container such as Tomcat 10.1.
4. Build the WAR with `mvn clean package`.
5. Deploy `target/securebank.war` to Tomcat.
6. Open `/securebank/api/accounts` on the local server.

The servlet API uses Jakarta namespaces and therefore requires Tomcat 10.1+ (not Tomcat 9).

## Java OOP concepts for your viva
- **Inheritance:** `SavingsAccount extends Account`; `CurrentAccount extends Account`.
- **Polymorphism:** a variable of type `Account` can hold either child object. Calling `withdraw()` invokes the child override at runtime.
- **Interface:** `Transferable` specifies a transfer contract independently of a particular account class.
- **Exception handling:** custom checked exceptions express invalid transfer amounts and insufficient funds.
- **Collections:** `Map<String, Account>` provides account lookup; `List<Transaction>` keeps transaction history; `Set<String>` helps avoid duplicate account numbers.
- **Hibernate:** entity classes map Java objects to relational database tables.
- **Servlet:** accepts HTTP requests and returns JSON.

## Current demo limitations
- The console demo is the reliable first checkpoint.
- The API endpoint is a starter demonstration, not a full customer login, beneficiary, KYC, or admin implementation.
- Before a real web deployment, add authentication, password hashing, role-based access control, CSRF protection, server-side validation, audit logs, database transactions and secure secrets management.


## Advanced login: email OTP and password reset

The login endpoint now expects server-side email delivery for OTP and reset codes. The UI calls `POST /api/auth` with these actions: `login`, `register`, `verify-otp`, `resend-otp`, `forgot-password`, `reset-password`, and `logout`.

### SMTP setup (Gmail example)

1. Enable 2-Step Verification on the Gmail account used by the project.
2. Create a Google App Password in Google Account security settings. Do not use your normal Gmail password.
3. Set these environment variables in the same Terminal session used to start Tomcat:

```bash
export SECUREBANK_SMTP_HOST="smtp.gmail.com"
export SECUREBANK_SMTP_PORT="587"
export SECUREBANK_SMTP_USER="your-project-email@gmail.com"
export SECUREBANK_SMTP_PASSWORD="your-16-character-app-password"
export SECUREBANK_SMTP_FROM="your-project-email@gmail.com"
```

4. Configure the existing database variables if needed:
```bash
export SECUREBANK_DB_URL="jdbc:mysql://localhost:3306/securebank?useSSL=false&serverTimezone=UTC"
export SECUREBANK_DB_USER="root"
export SECUREBANK_DB_PASSWORD="your-database-password"
```
5. Start Tomcat from that same Terminal session so it inherits the variables. Never commit credentials to Git or put them in JavaScript.

### Security notes

- OTP and password-reset codes expire after five minutes and are limited to five verification attempts per session.
- Login lockout is an in-memory, per-server-instance protection (five failed attempts, 15-minute lock). It resets if Tomcat restarts and is not a substitute for a persistent distributed rate limiter.
- HTTP session timeout is 15 minutes and the session cookie is HttpOnly. For production, serve only over HTTPS and configure Secure/SameSite cookies, CSRF protection, persistent rate limiting, email verification status, audit logging, and monitored secret storage.
- This is an educational banking prototype, not production banking software. Do not use real customer credentials or money.
