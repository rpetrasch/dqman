package org.dqman.config.camelprocessor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.dqman.model.DataIntegration;
import org.dqman.model.StepData;
import org.springframework.stereotype.Component;

/**
 * Processor to fetch data from a RDBMS.
 */
@Component
public class RdbmsDataProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        DataIntegration integration = exchange.getIn().getBody(DataIntegration.class);
        List<String> metadata = exchange.getIn().getHeader("metadata", List.class);
        if (integration == null || metadata == null) {
            throw new IllegalArgumentException("Integration cannot be null");
        }
        // Extract table and field names from tableFieldList
        Map<String, Set<String>> tableFields = new HashMap<>();
        for (String tableField : metadata) {
            // Eliminate all chars after the first "("
            tableField = tableField.substring(0, tableField.indexOf("("));
            tableField = tableField.trim();
            String[] tableFieldParts = tableField.split("\\.");
            tableFields.computeIfAbsent(tableFieldParts[0], k -> new HashSet<>()).add(tableFieldParts[1]);
        }

        // Key = table name, Value = list of rows, row = list of values (of columns)
        Map<String, List<List<String>>> data = new HashMap<>();

        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(
                integration.getUrl(), integration.getUser(), integration.getPassword())) {
            for (String tableName : tableFields.keySet()) {
                java.sql.Statement stmt = conn.createStatement();
                String fields = tableFields.get(tableName).stream()
                        .collect(Collectors.joining(", "));
                String query = "SELECT " + fields + " FROM " + tableName + " ORDER BY " + fields;
                java.sql.ResultSet rs = stmt.executeQuery(query);

                List<List<String>> tableData = new ArrayList<>();
                // header for table
                List<String> header = new ArrayList<>();
                for (String field : tableFields.get(tableName)) {
                    header.add(field);
                }
                tableData.add(header);
                while (rs.next()) {
                    List<String> rowData = new ArrayList<>();
                    for (String field : tableFields.get(tableName)) {
                        rowData.add(rs.getString(field));
                    }
                    tableData.add(rowData);
                }
                data.put(tableName, tableData);
            }
        }
        StepData result = new StepData(data);
        exchange.getIn().setBody(result);
    }
}
