package in.securebank.model;

import in.securebank.exception.InsufficientFundsException;
import in.securebank.exception.InvalidAmountException;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("CURRENT")
public class CurrentAccount extends Account {
    private double overdraftLimit;

    protected CurrentAccount() { }

    public CurrentAccount(String number, String owner, double openingBalance, double overdraftLimit) {
        super(number, owner, openingBalance);
        if (overdraftLimit < 0) throw new IllegalArgumentException("Overdraft limit cannot be negative.");
        this.overdraftLimit = overdraftLimit;
    }

    @Override
    public void withdraw(double amount)
            throws InvalidAmountException, InsufficientFundsException {
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new InvalidAmountException("Withdrawal amount must be greater than zero.");
        }
        if (getBalance() - amount < -overdraftLimit) {
            throw new InsufficientFundsException("Withdrawal exceeds the overdraft limit.");
        }
        setBalance(getBalance() - amount);
    }

    @Override
    protected boolean canWithdraw(double amount) {
        return Double.isFinite(amount) && amount > 0 && getBalance() - amount >= -overdraftLimit;
    }

    @Override
    public String getAccountType() { return "Current Account"; }

    public double getOverdraftLimit() { return overdraftLimit; }
}
