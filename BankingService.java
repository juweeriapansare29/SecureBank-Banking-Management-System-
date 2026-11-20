package in.securebank.service;

import in.securebank.exception.InsufficientFundsException;
import in.securebank.exception.InvalidAmountException;
import in.securebank.model.*;
import java.util.*;

/**
 * In-memory service for the console demo. A real web deployment should use
 * Hibernate repositories and database transactions rather than these collections.
 */
public class BankingService {
    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private final List<Transaction> transactions = new ArrayList<>();
    private final Set<String> knownAccountNumbers = new HashSet<>();

    public void addAccount(Account account) {
        Objects.requireNonNull(account, "Account is required");
        if (!knownAccountNumbers.add(account.getAccountNumber())) {
            throw new IllegalArgumentException("Duplicate account number: " + account.getAccountNumber());
        }
        accounts.put(account.getAccountNumber(), account);
    }

    public Account findAccount(String accountNumber) {
        Account account = accounts.get(accountNumber);
        if (account == null) throw new NoSuchElementException("Account not found: " + accountNumber);
        return account;
    }

    public void deposit(String accountNumber, double amount) throws InvalidAmountException {
        Account account = findAccount(accountNumber);
        account.deposit(amount);
        transactions.add(new DepositTransaction(
                reference("DEP"), amount, "Cash/demo deposit", account));
    }

    public void transfer(String fromNumber, String toNumber, double amount)
            throws InvalidAmountException, InsufficientFundsException {
        Account sender = findAccount(fromNumber);
        Account recipient = findAccount(toNumber);
        sender.transferTo(recipient, amount);
        transactions.add(new TransferTransaction(
                reference("TRF"), amount, "To " + recipient.getAccountNumber(), sender));
    }

    public Collection<Account> getAccounts() {
        return Collections.unmodifiableCollection(accounts.values());
    }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    private String reference(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
