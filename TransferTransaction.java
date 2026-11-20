package in.securebank.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("TRANSFER")
public class TransferTransaction extends Transaction {
    protected TransferTransaction() { }

    public TransferTransaction(String ref, double amount, String description, Account account) {
        super(ref, amount, description, account);
    }

    @Override
    public String describe() {
        return "Transfer of ₹" + String.format("%.2f", getAmount())
                + (getDescription() == null ? "" : " — " + getDescription());
    }
}
