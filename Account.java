package in.securebank.model;

import in.securebank.exception.InsufficientFundsException;
import in.securebank.exception.InvalidAmountException;
import jakarta.persistence.*;

@Entity
@Table(name = "bank_accounts")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "account_type")
public abstract class Account implements Transferable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 24)
    private String accountNumber;

    @Column(name = "owner_name", nullable = false, length = 100)
    private String ownerName;

    @Column(nullable = false, precision = 15, scale = 2)
    private double balance;

    protected Account() { } // Required by Hibernate

    protected Account(String accountNumber, String ownerName, double openingBalance) {
        if (openingBalance < 0) throw new IllegalArgumentException("Opening balance cannot be negative.");
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.balance = openingBalance;
    }

    public synchronized void deposit(double amount) throws InvalidAmountException {
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new InvalidAmountException("Deposit amount must be greater than zero.");
        }
        balance += amount;
    }

    public abstract void withdraw(double amount)
            throws InvalidAmountException, InsufficientFundsException;

    public abstract String getAccountType();

    @Override
    public synchronized void transferTo(Account recipient, double amount)
            throws InsufficientFundsException, InvalidAmountException {
        if (recipient == null) throw new IllegalArgumentException("Recipient account is required.");
        if (recipient == this) throw new IllegalArgumentException("Cannot transfer to the same account.");
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new InvalidAmountException("Transfer amount must be greater than zero.");
        }
        // Validate before mutating either account, avoiding a partial transfer.
        if (!canWithdraw(amount)) {
            throw new InsufficientFundsException("Insufficient available funds in " + accountNumber);
        }
        withdraw(amount);
        recipient.deposit(amount);
    }

    protected abstract boolean canWithdraw(double amount);

    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public String getOwnerName() { return ownerName; }
    public double getBalance() { return balance; }
    protected void setBalance(double balance) { this.balance = balance; }

    public String summary() {
        return getAccountType() + " | " + accountNumber + " | " + ownerName
                + " | Balance: ₹" + String.format("%.2f", balance);
    }
}
