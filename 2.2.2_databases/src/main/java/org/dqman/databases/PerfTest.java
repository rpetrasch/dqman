package org.dqman.databases;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.dqman.databases.Utils.transpose;

/**
 * Measurement of the duration of n operations
 */
public abstract class PerfTest {

    private List<List<Long>> resultsForOperation = new ArrayList<>(OperationType.values().length);

    abstract void insertTest(int n) throws Exception;

    abstract void deleteTest(int n) throws Exception;

    abstract void updateTest(int n) throws Exception;

    abstract void selectTest(int n) throws Exception;

    abstract void deleteAll() throws Exception;

    void addResult(List<Long> resultforN) {
        resultsForOperation.add(resultforN);
    }

    List<List<Long>> getResultsForOperation() {
        return transpose(resultsForOperation);
    }

    public void setResult(List<List<Long>> resultforN) {
        resultsForOperation = resultforN;
    }
}
