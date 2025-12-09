package org.dqman.databases;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;

import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cassandra performance test
 */
public class CassandraPerfTest extends PerfTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraPerfTest.class);

    public final CqlSession session;

    private static final String insertQuery = "INSERT INTO dq_keyspace.customer (id, name, age) VALUES (?, ?, ?)";
    private static final String updateQuery = "UPDATE dq_keyspace.customer SET age = ? WHERE id = ?";
    private static final String selectQuery = "SELECT * FROM dq_keyspace.customer WHERE id = ?";
    private static final String deleteQuery = "DELETE FROM dq_keyspace.customer WHERE id = ?";

    private PreparedStatement insertPreparedQuery;
    private PreparedStatement updatePreparedQuery;
    private PreparedStatement deletePreparedQuery;
    private PreparedStatement selectPreparedQuery ;

    public CassandraPerfTest(CqlSession session) {
        insertPreparedQuery = session.prepare(insertQuery);
        updatePreparedQuery = session.prepare(updateQuery);
        selectPreparedQuery = session.prepare(selectQuery);
        deletePreparedQuery = session.prepare(deleteQuery);
        this.session = session;
    }

    public void insertTest(int n) throws Exception {
        String insertQuery = "INSERT INTO dq_keyspace.customer (id, name, age) VALUES (?, ?, ?)";
        PreparedStatement insertPreparedQuery = session.prepare(insertQuery);
        for (int i = 1; i <= n; i++) {
            ResultSet resultSet = session.execute(insertPreparedQuery.bind( (long) i, "Name" + i, 20 + (i % 10))
                    .setConsistencyLevel(ConsistencyLevel.ANY));
            if (!resultSet.wasApplied()) {
                LOGGER.error("Insert failed: " + resultSet.getExecutionInfo());
                throw new Exception("Insert failed");
            }
        }
    }

    public void updateTest(int n) {
        for (int i = 1; i <= n; i++) {
            session.execute(updatePreparedQuery.bind(n, (long)i)
                    .setConsistencyLevel(ConsistencyLevel.ANY));
        }
    }

    public void deleteTest(int n) {
        for (int i = 1; i <= n; i++) {
            session.execute(deletePreparedQuery.bind((long) i)
                    .setConsistencyLevel(ConsistencyLevel.ANY));
        }
    }

    public void selectTest(int n) {
        String rowString;
        for (int i = 1; i <= n; i++) {
            for (Row row : session.execute(selectPreparedQuery.bind((long) i)
                    .setConsistencyLevel(ConsistencyLevel.LOCAL_ONE))) {
                long id = row.getLong("id");
                String name = row.getString("name");
                int age = row.getInt("age");
                rowString = "ID: " + id + ", Name: " + name + ", Age: " + age;
                LOGGER.debug(rowString);
            }
        }
    }

    @Override
    public void deleteAll() {
        String query = "TRUNCATE dq_keyspace.customer";
        PreparedStatement preparedQuery = session.prepare(query);
        session.execute(preparedQuery.bind());
    }

}

