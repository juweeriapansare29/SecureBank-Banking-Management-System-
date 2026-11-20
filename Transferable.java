package in.securebank.model;

import in.securebank.exception.InsufficientFundsException;
import in.securebank.exception.InvalidAmountException;

public interface Transferable {
    void transferTo(Account recipient, double amount)
            throws InsufficientFundsException, InvalidAmountException;
}
