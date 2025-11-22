package org.dqman.databases;

import java.sql.*;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PostgrSQL performance test
 */
public class PostgresPerfTest extends PerfTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresPerfTest.class);

    private final Connection connection;
    private int rowsAffected = 0;
    private boolean success = false;

    public PostgresPerfTest(Connection connection) {
        this.connection = connection;
    }

    public void insertTest(int n) throws Exception {
        String insertQuery = "INSERT INTO customer (id, name, age) VALUES (?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            for (int i = 1; i <= n; i++) {
                // Statement statement = connection.createStatement();
                // statement.execute(insertQuery);
                preparedStatement.setLong(1, i);
                preparedStatement.setString(2, "Name" + i);
                preparedStatement.setInt(3, 20 + (i % 10));
                success = preparedStatement.execute();
                if (!success) {
                    int rowsAffected = preparedStatement.getUpdateCount();
                    if (rowsAffected == 0) {
                        LOGGER.error("Insert failed: " + rowsAffected);
                        throw new Exception("Insert failed");
                    }
                }
                // preparedStatement.addBatch();
            }
            // preparedStatement.executeBatch();
        } catch (SQLException e) {
            LOGGER.error("Exception in insertTest: ",e);
            throw e;
        }
    }

    public void updateTest(int n) throws Exception {
        String updateQuery = "UPDATE customer SET age = age + 1 WHERE id = ?";
        int rowsAffected = 0;
        try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
            for (int i = 1; i <= n; i++) {
                preparedStatement.setInt(1, i);
                rowsAffected = preparedStatement.executeUpdate();
                if (rowsAffected == 0) {
                    LOGGER.error("Update failed: " + rowsAffected);
                    throw new Exception("Update failed");
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Exception in updateTest: ", e);
            throw e;
        }
    }

    public void deleteTest(int n) throws Exception {
        String deleteQuery = "DELETE FROM customer WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
            for (int i = 1; i <= n; i++) {
                preparedStatement.setInt(1, i);
                rowsAffected = preparedStatement.executeUpdate();
                if (rowsAffected == 0) {
                    LOGGER.error("Delete failed: " + rowsAffected);
                    throw new Exception("Delete failed");
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Exception in deleteTest: ", e);
            throw e;
        }
    }

    public void selectTest(int n) throws Exception {
        String selectQuery = "SELECT * FROM customer";
        String rowString;
        try (PreparedStatement preparedStatement = connection.prepareStatement(selectQuery);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                long id= resultSet.getLong("id");
                String name = resultSet.getString("name");
                int age = resultSet.getInt("age");
                rowString = "ID: " + id + ", Name: " + name + ", Age: " + age;
                LOGGER.debug(rowString);
            }
        } catch (SQLException e) {
            LOGGER.error("Exception in selectTest: ", e);
            throw e;
        }
    }

    public void deleteAll() throws Exception {
        String deleteQuery = "DELETE FROM customer";
        try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Exception in deleteAll: ", e);
            throw e;
        }
    }


}

