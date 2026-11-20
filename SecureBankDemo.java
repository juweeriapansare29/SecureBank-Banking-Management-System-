package in.securebank.demo;

import in.securebank.exception.*;
import in.securebank.model.*;
import in.securebank.service.BankingService;

public class SecureBankDemo {
    public static void main(String[] args) {
        BankingService bank = new BankingService();
        Account savings = new SavingsAccount("SB1001", "Aarav Sharma", 10000);
        Account current = new CurrentAccount("SB2001", "Priya Patil", 2000, 5000);

        bank.addAccount(savings);
        bank.addAccount(current);

        System.out.println("=== SecureBank OOP Demonstration ===");
        System.out.println("\\n1. POLYMORPHISM: same Account reference, different implementations");
        for (Account account : bank.getAccounts()) {
            System.out.println(account.summary());
            try {
                account.withdraw(1200);
                System.out.println("After withdrawal: " + account.summary());
            } catch (InvalidAmountException | InsufficientFundsException ex) {
                System.out.println("Transaction rejected: " + ex.getMessage());
            }
        }

        System.out.println("\\n2. INTERFACE + COLLECTIONS: transferring between accounts");
        try {
            bank.transfer("SB2001", "SB1001", 1500);
            System.out.println("Transfer successful.");
        } catch (InvalidAmountException | InsufficientFundsException ex) {
            System.out.println("Transfer rejected: " + ex.getMessage());
        }

        System.out.println("\\n3. EXCEPTION HANDLING: invalid amount");
        try {
            bank.deposit("SB1001", -50);
        } catch (InvalidAmountException ex) {
            System.out.println("Handled safely: " + ex.getMessage());
        }

        System.out.println("\\n4. TRANSACTION COLLECTION (polymorphic descriptions)");
        for (Transaction tx : bank.getTransactions()) {
            System.out.println(tx.getReferenceCode() + " -> " + tx.describe());
        }

        System.out.println("\\nFinal accounts:");
        for (Account account : bank.getAccounts()) {
            System.out.println(account.summary());
        }
    }
}
