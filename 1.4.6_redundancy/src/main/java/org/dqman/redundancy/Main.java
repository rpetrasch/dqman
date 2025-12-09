package org.dqman.redundancy;

import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException;

import java.util.logging.Logger;
import java.sql.*;
import java.io.File;

/**
 * Main class to demonstrate database access via SQL using transactions
 */
public class Main {
    static {
        LogConfig.configure();
    }
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    
    public static void main(String[] args) {
        // JDBC URL for embedded H2 database
        String url = "jdbc:h2:./testdb";
        String dbFile = "./testdb.mv.db";

        try (Connection conn = DriverManager.getConnection(url, "sa", "")) {

            // Create a table
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS customer (id INT PRIMARY KEY, name VARCHAR(255))");
            stmt.execute("CREATE TABLE IF NOT EXISTS customer_order (id INT PRIMARY KEY, customer_id INT, " +
                    "product VARCHAR(255), amount DECIMAL(10,2), " +
                    "FOREIGN KEY (customer_id) REFERENCES customer(id))");
            // Perform database operations
            // 1. Test: transaction and commit/rollback
            testPKViolation(conn, stmt);
            // 2. Test: relationship customer-order
            testCustomerOrderCreation(conn, stmt);
            // 3. Test: manipulation of FK for customer-order
            testCustomerOrderFK(conn, stmt);
            // 4. Test: deletion of customers
            testCustomerDel(conn, stmt);

            conn.setAutoCommit(true);
            LOGGER.info("Database operations completed");

        } catch (SQLException e) {
            LOGGER.severe("Database error: " + e.getMessage());
        } finally {
            // Delete database file after execution
            File db = new File(dbFile);
            if (db.exists() && db.delete()) {
                LOGGER.info("Database file deleted: " + dbFile);
            }
        }
    }

    /**
     * Test customer with violation of primary key constraint (uniqueness)
     * @param conn DB connection
     * @param stmt Statement
     * @throws SQLException DQL execution exception
     */
    private static void testPKViolation(Connection conn, Statement stmt) throws SQLException {
        // Transaction 1: Successful commit
        LOGGER.info("=== Transaction 1: Commit ===");
        conn.setAutoCommit(false);
        try {
            stmt.execute("INSERT INTO customer VALUES (1, 'Alice')");
            stmt.execute("INSERT INTO customer VALUES (2, 'Bob')");
            stmt.execute("INSERT INTO customer VALUES (4, 'Mary')");
            conn.commit();
            LOGGER.info("Insert for customer 1 and 2: transaction committed successfully.");
        } catch (SQLException e) {
            conn.rollback();
            LOGGER.warning("Transaction rolled back");
        }

        // Transaction 2: Rollback on error
        LOGGER.info("=== Transaction 2: Rollback ===");
        try {
            stmt.execute("INSERT INTO customer VALUES (3, 'Charlie')");
            stmt.execute("INSERT INTO customer VALUES (1, 'Duplicate')"); // This will fail (duplicate key)
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            LOGGER.warning("Insert for existing customer 1: Transaction rolled back due to error: " + e.getMessage());
        }

        // Query final data
        LOGGER.info("=== Final Data ===");
        ResultSet rs = stmt.executeQuery("SELECT * FROM customer");
        while (rs.next()) {
            LOGGER.info("ID: " + rs.getInt("id") + ", Name: " + rs.getString("name"));
        }
    }

    /**
     * Test relationship between customer and order (creation) with violation of foreign key constraint (referential integrity)
     * @param conn DB connection
     * @param stmt Statement
     * @throws SQLException DQL execution exception
     */
    private static void testCustomerOrderCreation(Connection conn, Statement stmt) throws SQLException {
        stmt.execute("INSERT INTO customer_order VALUES (101, 1, 'Laptop', 999.99)");
        stmt.execute("INSERT INTO customer_order VALUES (102, 2, 'Mouse', 29.99)");
        stmt.execute("INSERT INTO customer_order VALUES (103, 1, 'Keyboard', 79.99)");
        LOGGER.info("Customer orders 101, 102, and 103 created successfully.");
        try {
            stmt.execute("INSERT INTO customer_order VALUES (104, 42, 'Cable', 9.99)");
        } catch (JdbcSQLIntegrityConstraintViolationException e) {
            LOGGER.severe("Cannot insert order for non existing customer with id 42: " + e.getMessage());
        }
    }

    /**
     * Test order update with violation of foreign key constraint (referential integrity)
     * @param conn DB connection
     * @param stmt Statement
     * @throws SQLException DQL execution exception
     */
    private static void testCustomerOrderFK(Connection conn, Statement stmt) throws SQLException {
        stmt.execute("UPDATE customer_order SET customer_id = 2 WHERE id =101");
        LOGGER.info("Customer orders 101 assigned to customer 2 successfully.");
        try {
            stmt.execute("UPDATE customer_order SET customer_id = 42 WHERE id =101");
        } catch (JdbcSQLIntegrityConstraintViolationException e) {
            LOGGER.severe("Cannot update order for non existing customer with id 42: " + e.getMessage());
        }
    }

    /**
     * Test customer deletion with violation of existing foreign key (referential integrity)
     * @param conn DB connection
     * @param stmt Statement
     * @throws SQLException DQL execution exception
     */
    private static void testCustomerDel(Connection conn, Statement stmt) throws SQLException {
        // The execute() method returns a boolean that indicates the type of result, not whether it succeeded
        // => use executeUpdate to directly get the affected row
        int rowsDeleted = stmt.executeUpdate("DELETE FROM customer WHERE id = 4");
        LOGGER.info("Customer with id 4 deleted successfully, deleted rows: " + rowsDeleted);
        try {
            stmt.execute("DELETE FROM customer WHERE id = 2");
        } catch (JdbcSQLIntegrityConstraintViolationException e) {
            LOGGER.severe("Cannot delete customer who has order: " + e.getMessage());
        }
    }

}
