package org.dqman.acid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

/**
 * Bank service for transferring money between accounts.
 */
@Service // Spring service bean
public class BankService {

    @Autowired
    private AccountRepository accountRepository;

    /**
     * Transfer money between two accounts.
     * With annotation @Transactional: the entire method is a single, atomic transaction, i.e.,
     * spring commits the transaction at the ende if no exceptions were thrown
     * @param fromAccountHolder holder of the account to transfer from
     * @param toAccountHolder holder of the account to transfer to
     * @param amount amount to transfer
     */
    //@Transactional
    public void transferMoney(String fromAccountHolder, String toAccountHolder, BigDecimal amount) {

        // 1. Find the accounts (entities) and check if they exist
        Account fromAccount = accountRepository.findByHolder(fromAccountHolder);
        Account toAccount = accountRepository.findByHolder(toAccountHolder);
        if (fromAccount == null || toAccount == null) {
            // throw new IllegalArgumentException("Account not found");  // Test transaction failure
        }

        // 2. Check the balance for insufficient funds
        if (fromAccount.getBalance().subtract(amount).compareTo(BigDecimal.ZERO) < 0) {
            // throw new IllegalArgumentException("Insufficient funds"); // Test transaction failure
        }
        // 3. Transfer the money
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        // 4. Save the changes
        // With JPA, these changes are often flushed to the DB at the end of the transaction automatically (dirty checking).
        accountRepository.save(toAccount);  // change order of saving accounts to test transaction failure
        accountRepository.save(fromAccount);
    }
}
