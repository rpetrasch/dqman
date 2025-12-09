package org.dqman.acid;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Application ready listener for tests of the product audit history
 */
@Component
public class AppReadyListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(AppReadyListener.class);
    @Autowired
    private BankService bankService;

    @Autowired
    private AccountRepository accountRepository;

    /**
     * Run post-init code when the application is ready.
     * Run tests for the bank service (ACID)
     * @param event
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        LOG.info("Application is ready. Running post-init code: create accounts and transfer money...");

        BigDecimal amountA = new BigDecimal(100);
        BigDecimal amountB = new BigDecimal(20);
        BigDecimal expectedSum = amountA.add(amountB);
        BigDecimal amountTransfer = new BigDecimal(80);

        LOG.info("--- Test 1: save account B, transfer money");
        BigDecimal sum = createAndTransferMoney(true, amountA, amountB, amountTransfer);
        checkAndLogSum(sum, expectedSum);

        LOG.info("--- Test 2: do not save account B, transfer money");
        sum = createAndTransferMoney(false, amountA, amountB, amountTransfer);
        checkAndLogSum(sum, expectedSum);

        LOG.info("--- Test 3: not sufficient funds for account A, transfer money");
        amountTransfer = amountTransfer.add(amountA);
        sum = createAndTransferMoney(true, amountA, amountB, amountTransfer);
        checkAndLogSum(sum, expectedSum);
        LOG.info("Finished");
    }

    /**
     * Create accounts and transfer money
     * @param saveAccountB save account B if true, otherwise use account B with id 999999
     * @param amountA initial balance for account A
     * @param amountTransfer transfer amount for account A to account B
     * @return balance of account A plus account B
     */
    private BigDecimal createAndTransferMoney(boolean saveAccountB, BigDecimal amountA, BigDecimal amountB, BigDecimal amountTransfer) {
        String accountHolderA = "A";
        String accountHolderB = "B";

        accountRepository.deleteAll();
        Account accountA = new Account(accountHolderA, amountA);
        accountA = accountRepository.save(accountA);
        LOG.info("Account A id: " + accountA.getId());

        Account accountB = new Account(accountHolderB, amountB);
        if (saveAccountB) accountB = accountRepository.save(accountB);
        else accountB.setId(999999L);
        LOG.info("Account B id: " + accountB.getId());

        List<Account> accountList = accountRepository.findAll();
        LOG.info("Account list: " + accountList.size());

        try {
            bankService.transferMoney(accountHolderA, accountHolderB, amountTransfer);
            LOG.info("Transfer successful");
        } catch (Exception e) {
            LOG.error("Transfer failed: " + e.getMessage());
        }

        BigDecimal balanceAccountA = accountRepository.findById(accountA.getId()).orElseThrow().getBalance();
        LOG.info("Amount A: " + balanceAccountA);
        Optional<Account> optionalAccount = accountRepository.findById(accountB.getId());
        BigDecimal sum = balanceAccountA;
        if (optionalAccount.isPresent()) {
            Account accB = optionalAccount.get();
            sum = sum.add(accB.getBalance());
            LOG.info("Amount B: " + accB.getBalance());
        } else {
            LOG.info("Amount B: ---");
        }
        return sum;
    }

    /**
     * Check if the sum of balances is correct
     * @param sum of balances (actual value)
     * @param expectedSum expected sum of balances
     */
    private void checkAndLogSum(BigDecimal sum, BigDecimal expectedSum) {
        // equals may not work: BigDecimal distinguishes between the scale (number of decimal places), not just the value.
        if (sum.compareTo(expectedSum) == 0) {
            LOG.info("Sum of balances is correct: " + sum);
        } else {
        LOG.error("Sum of balances is incorrect: " + sum + ", expected: " + expectedSum);
    }
        }

}
