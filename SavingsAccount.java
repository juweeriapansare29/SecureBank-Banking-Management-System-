package in.securebank.model;

import in.securebank.exception.InsufficientFundsException;
import in.securebank.exception.InvalidAmountException;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("SAVINGS")
public class SavingsAccount extends Account {
    private static final double MINIMUM_BALANCE = 500.00;

    protected SavingsAccount() { }

    public SavingsAccount(String number, String owner, double openingBalance) {
        super(number, owner, openingBalance);
    }

    @Override
    public void withdraw(double amount)
            throws InvalidAmountException, InsufficientFundsException {
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new InvalidAmountException("Withdrawal amount must be greater than zero.");
        }
        if (getBalance() - amount < MINIMUM_BALANCE) {
            throw new InsufficientFundsException(
                    "Savings account must retain the minimum balance of ₹" + MINIMUM_BALANCE);
        }
        setBalance(getBalance() - amount);
    }

    @Override
    protected boolean canWithdraw(double amount) {
        return Double.isFinite(amount) && amount > 0 && getBalance() - amount >= MINIMUM_BALANCE;
    }

    @Override
    public String getAccountType() { return "Savings Account"; }
}
