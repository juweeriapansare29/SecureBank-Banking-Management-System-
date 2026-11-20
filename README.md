# SecureBank — All-in-One Academic Project

Responsive banking UI + Android WebView wrapper + Java Servlet/Hibernate/JDBC starter + MySQL schema. All banking activity is simulated; there is no real payment integration.

## What was improved in this update

- Added Smart Savings goals with target, saved amount, time horizon and monthly target calculation.
- Added Expense Tracker category breakdown.
- Added rule-based demo Fraud Alerts for large and repeated outgoing amounts (not production fraud detection).
- Added Budget Planner with spending progress.
- Added a clearly illustrative Credit Score Simulator; it is not a bureau score.
- Fixed dashboard balance math using an opening balance plus ledger transactions. The seeded ledger now reconciles to the displayed starting balance.
- Transaction search no longer navigates to another page while typing. Added category and date filters; CSV includes reference/status fields.
- Transfer now has a review confirmation, input validation, generated demo reference, and receipt. No real money moves.
- Added JDBC-backed `/api/auth` Servlet for customer registration/login/logout/session lookup. Passwords are stored using PBKDF2-HMAC-SHA256; database configuration is read from environment variables.
- Added `/api/admin/customers`, which checks the server-side session role before returning customer/KYC records.
- Extended MySQL schema for customers, beneficiaries, goals, budgets and KYC requests.
- Synced the shared responsive UI to the website, Java webapp, and Android assets.

## Run the UI preview

Open `web/index.html` in a browser. When opened as a local file, the app runs in local demo mode; this mode does not provide server authentication or persistent database storage. The Android project loads the same UI from local assets.

## Configure the Java backend

Requirements: JDK 17+, Maven, MySQL 8+, and a Jakarta Servlet 6 compatible container (for example Tomcat 10.1+).

1. Create the database and tables by running `java-backend/database/schema.sql` in MySQL.
2. Configure environment variables before starting the servlet container:
   - `SECUREBANK_DB_URL` (default `jdbc:mysql://localhost:3306/securebank?useSSL=false&serverTimezone=UTC`)
   - `SECUREBANK_DB_USER` (default `root`)
   - `SECUREBANK_DB_PASSWORD` (set your own local password; do not commit secrets)
3. From `java-backend`, run `mvn package`.
4. Deploy `target/securebank.war` to Tomcat 10.1+ and open the app under its context path. The UI calls the relative `api/auth` endpoint when served over HTTP(S).
5. Use `/api/auth` with POST form fields `action=register|login`, `email`, `password`, and for registration `name`. POST `action=logout` to end the session; GET `/api/auth` returns the current session identity. `/api/admin/customers` requires an `ADMIN` session.

To create an administrator for an academic demo, register a normal account and change its role manually in your local database using a trusted SQL client. Do not expose this demo database or admin account publicly.

## Important project scope / limitations

This remains an academic prototype, not a production banking system. The new savings, expense, budget, credit-score and fraud features currently calculate in the browser from demo data. The new JDBC auth and admin endpoints are database-backed, but beneficiaries, KYC, account balances, and transaction mutations are not yet fully routed through a unified server-side service; do not treat local UI state as authoritative. Existing Hibernate OOP models and account endpoint remain as teaching examples. Production use would require HTTPS, CSRF protection, secure cookie settings, rate limiting, audit logging, transaction locking/atomic DB updates, authorization checks on every customer-owned record, migrations, and automated integration tests.

## Java OOP topics

`Account` is the abstract parent for `SavingsAccount` and `CurrentAccount`; transaction types demonstrate inheritance; `Transferable` demonstrates an interface; custom exceptions demonstrate exception handling; collections are used in the demo service. See `java-backend/README.md` for the original starter notes.


## UI/UX update: SecureBot and Smart Interest Lab

The web, Java webapp, and Android WebView UI now include a rule-based SecureBot FAQ widget and an illustrative Smart Interest Lab. The chatbot is a frontend FAQ prototype, not a live AI service. The interest calculator uses illustrative rates and is not connected to a real account. Rebuild the WAR from `java-backend` (`mvn clean package`) and redeploy `target/securebank.war` to Tomcat to see the webapp changes. For the local UI preview, open `web/index.html`.
