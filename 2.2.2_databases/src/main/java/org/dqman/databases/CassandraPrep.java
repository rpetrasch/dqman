package org.dqman.databases;

import java.net.InetSocketAddress;
import java.sql.SQLException;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cassandra data prep for performance test
 */
public class CassandraPrep {
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraPrep.class);

    private CqlSession cqlSession = null;

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 28201;
    private static final String DATACENTER = "datacenter1";
    private static final String USERNAME = "cassandra";
    private static final String PASSWORD = "cassandra";
    private static final String KEYSPACE = "dq_keyspace";
    public static final CassandraPrep INSTANCE = new CassandraPrep();

    private CassandraPrep() {
    }

    public CqlSession connect() {
        if (cqlSession == null) {
            cqlSession = CqlSession.builder()
                    .addContactPoint(new InetSocketAddress(HOST, PORT))
                    // .withAuthCredentials(USERNAME, PASSWORD)   // Uncomment this line if you have authentication enabled
                    .withLocalDatacenter(DATACENTER)
                    .withKeyspace(KEYSPACE)
                    .build();
        }
        return cqlSession;
    }

    public void prepare() {
        connect();
        LOGGER.info("Connected to Cassandra: " + cqlSession.getMetadata().getClusterName());
        // drop and create a keyspace
        String dropKeyspaceQuery = "DROP KEYSPACE IF EXISTS dq_keyspace";
        cqlSession.execute(dropKeyspaceQuery);
        String createKeyspaceQuery = "CREATE KEYSPACE IF NOT EXISTS dq_keyspace "
                + "WITH replication = {'class':'SimpleStrategy', 'replication_factor':1}";
        ResultSet resultSet = cqlSession.execute(createKeyspaceQuery);
        LOGGER.info("Keyspace created: " + resultSet.getExecutionInfo());
        // session.execute("USE dq_keyspace"); // This is an anti-pattern
        // ToDo use UUID
        String createTableQuery = "CREATE TABLE IF NOT EXISTS customer (id bigint PRIMARY KEY, name text, age int)";
        resultSet = cqlSession.execute(createTableQuery);
        LOGGER.info("Table created: " + resultSet.getExecutionInfo());
//            String insertQuery = "INSERT INTO customer (id, name, age) VALUES (uuid(), 'Alice', 30)";
//            resultSet =session.execute(insertQuery);
//            System.out.println("Data inserted: " + resultSet.getExecutionInfo());

    }

    public void close() {
        cqlSession.close();
    }
}
