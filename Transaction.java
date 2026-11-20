package in.securebank.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_transactions")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "transaction_type")
public abstract class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_code", nullable = false, unique = true, length = 40)
    private String referenceCode;

    @Column(nullable = false, precision = 15, scale = 2)
    private double amount;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    protected Transaction() { }

    protected Transaction(String referenceCode, double amount, String description, Account account) {
        this.referenceCode = referenceCode;
        this.amount = amount;
        this.description = description;
        this.account = account;
    }

    public abstract String describe();

    public Long getId() { return id; }
    public String getReferenceCode() { return referenceCode; }
    public double getAmount() { return amount; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Account getAccount() { return account; }
}
