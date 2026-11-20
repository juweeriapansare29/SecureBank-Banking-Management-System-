# Project structure

- `model/Account.java`: abstract parent class; common balance/account fields
- `model/SavingsAccount.java`: child class with minimum-balance withdrawal rule
- `model/CurrentAccount.java`: child class with overdraft rule
- `model/Transferable.java`: transfer interface
- `model/Transaction.java`: abstract transaction parent
- `model/DepositTransaction.java`, `TransferTransaction.java`: transaction subclasses
- `exception/`: custom checked exceptions
- `service/BankingService.java`: collections-based demo business logic
- `demo/SecureBankDemo.java`: console demonstration for viva
- `persistence/HibernateUtil.java`: Hibernate session factory
- `web/AccountsServlet.java`: sample Servlet API endpoint
- `database/schema.sql`: MySQL starter schema
- `src/main/webapp/`: web entry point
