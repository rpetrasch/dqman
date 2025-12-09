package org.dqman.databases;

import java.sql.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PostgrSQL data prep for performance test
 */
public class PostgresPrep {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresPrep.class);

    Connection connection = null;

    private static final String HOST = "localhost";
    private static final int PORT = 28100;
    private static final String DATABASE = "dataquality";
    private static final String USERNAME = "dquser";
    private static final String PASSWORD = "dqpassword";
    public static final PostgresPrep INSTANCE = new PostgresPrep();

    private PostgresPrep() {
    }

    public Connection connect() {
        if (connection == null) {
            String postgresUrl = "jdbc:postgresql://" + HOST + ":" + PORT + "/" + DATABASE + "?tcpKeepAlive=true";
            try {
                connection = DriverManager.getConnection(postgresUrl, USERNAME, PASSWORD);
            } catch (SQLException e) {
                LOGGER.error("Exception in Postgres connect: ", e);
            }
        }
        return connection;
    }

    public void prepare() throws SQLException {
        // try (Connection postgresConnection = connect()) {
        connect();
        LOGGER.info("Connected to PostgreSQL: " + connection.getMetaData());
        try (Statement stmt = connection.createStatement()) {
            // drop and create a table
            stmt.execute("DROP TABLE IF EXISTS customer");

            // ToDo use UUID
            stmt.execute("CREATE TABLE IF NOT EXISTS customer (id BIGINT PRIMARY KEY, name TEXT, age INT)");
//                stmt.execute("INSERT INTO customer (name, age) VALUES ('Bob', 25)");
//                ResultSet rs = stmt.executeQuery("SELECT * FROM users");
//                LOGGER.info("PostgreSQL data:");
//                while (rs.next()) {
//                    LOGGER.info("ID: " + rs.getInt("id") + ", Name: " + rs.getString("name") + ", Age: " + rs.getInt("age"));
//                }
        }
//        } catch (Exception e) {
//            LOGGER.error("Error preparing Cassandra", e);
//        }
    }

    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            LOGGER.error("Exception in Postgres close: ", e);
        }
    }
}
