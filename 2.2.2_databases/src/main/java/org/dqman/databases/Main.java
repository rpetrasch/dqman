package org.dqman.databases;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DriverExecutionProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;

import static com.datastax.oss.driver.api.core.config.DefaultDriverOption.*;

/**
 * Main class to run the performance test
 */
public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static final int N = 10000;
    private static final int STEP = 100;

    /**
     * Main method to run the performance test
     * @param args runtime arguments
     * @throws Exception if an error occurs
     */
    public static void main(String[] args) throws Exception {
        debugCassandraConfig();
        try {
            run();
            LOGGER.info("Test finished: ");
        } catch (Exception e) {
            LOGGER.error("Error in main: ", e);
        }
    }

    /**
     * Run the performance test
     * Outer loop: for all performance test objects (Postgres, Cassandra) run the test for all operation types (insert, update, delete, select)
     * Inner loop: for a specific n (number of datasets) run the test for all operation types
     *
     * @throws Exception
     */
    private static void run() throws Exception {
        CassandraPrep.INSTANCE.prepare();
        PostgresPrep.INSTANCE.prepare();

        List<PerfTest> perfTestList = new ArrayList<>();
        perfTestList.add(new PostgresPerfTest(PostgresPrep.INSTANCE.connect()));
        perfTestList.add(new CassandraPerfTest(CassandraPrep.INSTANCE.connect()));

        for (PerfTest test : perfTestList) { // loop: measure duration for all operation types for a specific n (number of datasets)
            for (int n = STEP; n <= N; n += STEP) {
                test.addResult(executeTestForAllOperations(test, n));
                test.deleteAll();
            }
        }

        perfTestList.forEach(perfTest -> {
            LOGGER.info("Test classs: " + perfTest.getClass().getSimpleName());
            List<List<Long>> resultForPerfText = perfTest.getResultsForOperation();

            for (int opertionId = 0; opertionId < OperationType.values().length; opertionId++) {
                List<Long> resultForOperation = resultForPerfText.get(opertionId);
                LOGGER.info(OperationType.values()[opertionId] + ": ");
                StringBuilder desmosPoints = new StringBuilder();
                for (int i = 0; i < resultForOperation.size(); i++) {
                    // LOGGER.info("n = " + ((i+1) * STEP) + ": " + resultForOperation.get(i) + "ms");
                    desmosPoints.append("(").append((i+1) * STEP).append(",").append(resultForOperation.get(i)).append("),");
                }
                LOGGER.info("Desmos points: " + desmosPoints);
            }
        });
        CassandraPrep.INSTANCE.close();
        PostgresPrep.INSTANCE.close();
    }

    /**
     * Outer loop: Test for a performance test object (Postgres, Cassandra=: for every operation type (insert, update, delete, select)
     * n operations are performed: call to executeTest(perfTest, n, operationType)
     *
     * @param perfTest
     * @param n
     * @return
     */
    private static List<Long> executeTestForAllOperations(PerfTest perfTest, int n) throws Exception {
        List<Long> resultForOperation = new ArrayList<>(OperationType.values().length);
        for (OperationType operationType : OperationType.values()) {
            Long result = executeTest(perfTest, n, operationType);
            resultForOperation.add(result);
        }
        return resultForOperation;
    }

    /**
     * Inner loop: executes one operation of a certain operation type (insert, update, delete, select) and measures the
     * duration
     *
     * @param perfTest      test execution object
     * @param n             number of
     * @param operationType
     * @return
     */
    private static Long executeTest(PerfTest perfTest, int n, OperationType operationType) throws Exception {
        long startTime, endTime, duration;
        startTime = System.currentTimeMillis();
        switch (operationType) {
            case INSERT -> perfTest.insertTest(n);
            case UPDATE -> perfTest.updateTest(n);
            case SELECT -> perfTest.selectTest(n);
            case DELETE -> perfTest.deleteTest(n);
        }
        endTime = System.currentTimeMillis();
        duration = endTime - startTime;
        // LOGGER.info("time: " + duration + " ms");
        return duration;
    }

    /**
     * Debug Cassandra configuration
     */
    public static void debugCassandraConfig() {
        CqlSession session = CassandraPrep.INSTANCE.connect();
        DriverExecutionProfile profile = session.getContext().getConfig().getDefaultProfile();

        // Using DriverOption to access configuration values
        LOGGER.debug("Contact Points: " + profile.getStringList(CONTACT_POINTS));
        LOGGER.debug("Local Datacenter: " + profile.getString(LOAD_BALANCING_LOCAL_DATACENTER));
        // Retrieve pool configuration
        int localPoolSize = profile.getInt(CONNECTION_POOL_LOCAL_SIZE);
        int remotePoolSize = profile.getInt(CONNECTION_POOL_REMOTE_SIZE);
        LOGGER.debug("Local Pool Size: " + localPoolSize);
        LOGGER.debug("Remote Pool Size: " + remotePoolSize);
    }
}




