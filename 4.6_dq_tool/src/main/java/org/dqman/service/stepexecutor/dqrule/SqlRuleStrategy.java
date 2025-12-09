package org.dqman.service.stepexecutor.dqrule;

import java.util.List;
import java.util.Map;

import org.dqman.model.DqFlowStep;
import org.dqman.model.DqRule;
import org.dqman.model.FlowExecution;
import org.dqman.model.StepData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

/**
 * Strategy implementation for SQL-based DQ rules
 */
@Component
@Slf4j
public class SqlRuleStrategy implements DqRuleStrategy {

    @Override
    // Only use Transactioal with temporary tables,
    // no Transactional = immediate commit
    // @Transactional
    public StepData execute(DqFlowStep step, DqRule rule, FlowExecution context) {
        log.info("Executing SQL rule: {} for step ID: {}", rule.getName(), step.getId());

        try {
            // Get the SQL query from rule.getRuleValue()
            String sqlQuery = rule.getRuleValue();
            log.debug("SQL query to execute: {}", sqlQuery);

            // Get the source data from predecessor steps (not from current step!)
            StepData sourceData = context.getPreviousData(step);
            log.debug("Source data from predecessor step(s): {}", sourceData);

            if (sourceData == null || sourceData.data() == null || sourceData.data().isEmpty()) {
                log.warn("No source data available from predecessor steps");
                return new StepData("error", "No source data available to execute SQL query");
            }

            // Create JdbcTemplate from the DataSource
            JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getDataSource());
            log.info("Creating temporary tables");
            Map<String, String> tableNames = createTemporaryTables(jdbcTemplate, context.getFlowId(), context.getId(),
                    step.getId(), sourceData);

            // Replace table names in the SQL query (temporary tables are named like:
            // sourceDataName_flowId_flowExecutionId_stepId)
            boolean replaced = false;
            for (Map.Entry<String, String> entry : tableNames.entrySet()) {
                if (sqlQuery.contains(entry.getKey())) {
                    sqlQuery = sqlQuery.replace(entry.getKey(), entry.getValue());
                    replaced = true;
                }
            }
            // If no table names were replaced, return error
            if (!replaced) {
                log.warn("No table names found in SQL query: {}", sqlQuery);
                return new StepData("error", "No table names found in SQL query");
            }

            // Execute the SQL query against the temporary table
            log.info("Executing SQL query: {}", sqlQuery);
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sqlQuery);

            log.info("SQL query executed successfully. Rows returned: {}", results.size());

            // ToDo: Check result
            // a) 0 rows returned = no error -> DQ_PASS
            // b) > 0 rows returned = error -> DQ_FAIL

            // Convert results to StepData format
            // ToDo: Improve this conversion to handle multiple columns properly
            return convertResultsToStepData(rule, results);

        } catch (Exception e) {
            log.error("Error executing SQL rule: {}", rule.getName(), e);
            return new StepData("error", "SQL execution failed: " + e.getMessage());
        }
    }

    /**
     * Creates temporary tables in H2 from StepData
     * 
     * @return Map of original table names to temporary table names
     */
    private Map<String, String> createTemporaryTables(JdbcTemplate jdbcTemplate, Long flowId, Long flowExecutionId,
            Long stepId, StepData sourceData) {

        Map<String, String> tableNameMapping = new java.util.HashMap<>();

        for (Map.Entry<String, List<List<String>>> entry : sourceData.data().entrySet()) {

            // Create temporary table name
            String tableName = entry.getKey() + "_" + flowId + "_" + flowExecutionId + "_" + stepId;
            List<List<String>> rows = entry.getValue(); // table data
            // ToDo extract data type, e.g. VARCHAR, INT, etc. from first row

            // Drop table if it exists
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + tableName);

            // Assume first row is header
            List<String> headers = rows.get(0);

            // Create table with columns (all as VARCHAR for simplicity)
            // permanent table. Use "CREATE TEMPORARY TABLE " to create temporary table that
            // will be deleted at the end of the flow execution
            String createTableSqlClause = "CREATE TABLE";

            StringBuilder createTableSql = new StringBuilder(createTableSqlClause + " " + tableName + " (");
            for (int i = 0; i < headers.size(); i++) {
                if (i > 0)
                    createTableSql.append(", ");
                createTableSql.append(headers.get(i)).append(" VARCHAR(1000)");
            }
            createTableSql.append(")");

            log.debug("Creating table with SQL: {}", createTableSql);
            jdbcTemplate.execute(createTableSql.toString());

            // Insert data rows (skip header row)
            for (int i = 1; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                StringBuilder insertSql = new StringBuilder("INSERT INTO " + tableName + " VALUES (");
                for (int j = 0; j < row.size(); j++) {
                    if (j > 0)
                        insertSql.append(", ");
                    insertSql.append("?");
                }
                insertSql.append(")");

                int rowNum = jdbcTemplate.update(insertSql.toString(), row.toArray());
                log.debug("Inserted {} rows into temporary table {}", rowNum, tableName);
            }

            // Add mapping from original name to temporary table name
            tableNameMapping.put(entry.getKey(), tableName);
        }
        return tableNameMapping;
    }

    /**
     * Converts SQL query results to StepData format
     * 
     * @param rule    DqRule object
     * @param results List of maps containing the results of the SQL query
     * @return StepData object containing the results
     */
    private StepData convertResultsToStepData(DqRule rule, List<Map<String, Object>> results) {
        String resultMessage = rule.getId() + " " + rule.getName();
        if (results == null || results.isEmpty()) {
            return new StepData(resultMessage, "No rows returned");
        }

        // Convert to List<List<String>> format
        List<List<String>> rows = new java.util.ArrayList<>();

        // Add header row, ToDo get(0) is not safe, mult. columns possible
        List<String> headers = new java.util.ArrayList<>(results.get(0).keySet());
        rows.add(headers);

        // Add data rows
        for (Map<String, Object> row : results) {
            List<String> rowData = new java.util.ArrayList<>();
            for (String header : headers) {
                Object value = row.get(header);
                rowData.add(value != null ? value.toString() : "");
            }
            rows.add(rowData);
        }

        return new StepData(resultMessage, rows);
    }

    @Override
    public String getSupportedRuleType() {
        return "SQL";
    }
}
