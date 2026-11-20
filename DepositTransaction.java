package in.securebank.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("DEPOSIT")
public class DepositTransaction extends Transaction {
    protected DepositTransaction() { }

    public DepositTransaction(String ref, double amount, String description, Account account) {
        super(ref, amount, description, account);
    }

    @Override
    public String describe() {
        return "Deposit of ₹" + String.format("%.2f", getAmount());
    }
}
