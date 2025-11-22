SQL Examples
In SQL, you control transactions manually using BEGIN, COMMIT, and ROLLBACK. You can also set the isolation level for your session or for a specific transaction.
1. Basic Transaction Control
   Let's use our bank transfer example.
   SQL
   -- Start the transaction
   BEGIN TRANSACTION;

TRY
-- Operation 1: Debit Alice
UPDATE Accounts
SET balance = balance - 100
WHERE account_holder = 'Alice';

    -- Operation 2: Credit Bob
    UPDATE Accounts
    SET balance = balance + 100
    WHERE account_holder = 'Bob';

    -- If both succeeded, commit the changes
    COMMIT TRANSACTION;
    PRINT 'Transfer successful.';

CATCH
-- If any error occurred, roll back everything
ROLLBACK TRANSACTION;
PRINT 'Transfer failed. No changes were made.';
END CATCH;
Note: The TRY...CATCH syntax is T-SQL (SQL Server). In PostgreSQL, you would use a BEGIN...EXCEPTION...END; block, and in MySQL, you might use handler logic within a stored procedure.
2. Setting the Isolation Level
   You can set the isolation level before your transaction begins.
   SQL
   -- Set the level for the current session (all future transactions)
   SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;

-- Or, set the level for just the *next* transaction
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;

BEGIN TRANSACTION;
-- ... Your SQL operations ...
COMMIT;

Java (JPA) with Spring Examples
In Java, especially when using frameworks like Spring with JPA (Java Persistence API), transaction management is often handled declaratively with annotations. This is far more common and robust than managing it manually.
The @Transactional annotation from Spring (org.springframework.transaction.annotation.Transactional) is the key.
1. Basic Declarative Transaction
   By adding @Transactional to a public method, Spring automatically wraps that method in a transaction. It will BEGIN the transaction when the method is called and COMMIT it when the method returns successfully.
   Java
   @Service // Marks this as a Spring service bean
   public class BankService {

   @Autowired
   private AccountRepository accountRepository;

   // This entire method is a single, atomic transaction.
   @Transactional
   public void transferMoney(String fromAccountHolder, String toAccountHolder, BigDecimal amount) {

        // 1. Find the accounts (entities)
        Account fromAccount = accountRepository.findByHolder(fromAccountHolder);
        Account toAccount = accountRepository.findByHolder(toAccountHolder);

        // 2. Perform the logic (operations)
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        // 3. Save the changes
        // With JPA, these changes are often flushed to the DB at the end
        // of the transaction automatically (dirty checking).
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

   } // <-- Spring commits the transaction here if no exceptions were thrown
   }
2. Handling Rollbacks
   By default, Spring's @Transactional will automatically roll back the transaction if a RuntimeException (an unchecked exception) is thrown. It will not roll back on a checked Exception (like IOException).
   Java
   @Transactional
   public void transferMoney(String from, String to, BigDecimal amount) {

   Account fromAccount = accountRepository.findByHolder(from);

   if (fromAccount.getBalance().compareTo(amount) < 0) {
   // This is a RuntimeException.
   // It will trigger an automatic ROLLBACK.
   throw new InsufficientFundsException("Not enough money in the account.");
   }

   // ... rest of the transfer logic ...

} // <-- Spring rolls back the transaction here due to the exception
You can customize this behavior:
Java
// Roll back for a specific checked exception
@Transactional(rollbackFor = BankServerDownException.class)
public void doRiskyOperation() throws BankServerDownException {
// ...
}

// Never roll back, even for a RuntimeException (rarely a good idea)
@Transactional(noRollbackFor = InsufficientFundsException.class)
public void doReportOperation() {
// ...
}
3. Setting the Isolation Level in JPA
   You can easily set the isolation level as an attribute of the annotation. This is extremely useful for optimizing performance. You can use a safe, high level for critical writes and a fast, low level for simple read operations.
   Java
   @Service
   public class ReportService {

   // For a financial report, we might be okay with reading data
   // that is slightly out of date, but we can't tolerate waiting
   // for write-locks. READ_COMMITTED is a good default.
   @Transactional(isolation = Isolation.READ_COMMITTED, readOnly = true)
   public Report generateQuarterlyReport() {
   // ... complex read-only queries ...
   // 'readOnly = true' is a further optimization hint
   }
   }

@Service
public class BankService {

    // For the critical transfer, we want to prevent non-repeatable reads.
    // REPEATABLE_READ ensures that once we read the balance,
    // it won't be changed by another transaction.
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void transferMoney(String from, String to, BigDecimal amount) {
        // ... transfer logic ...
    }
}