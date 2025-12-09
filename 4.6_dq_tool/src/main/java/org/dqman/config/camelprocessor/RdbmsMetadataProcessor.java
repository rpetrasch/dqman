package org.dqman.config.camelprocessor;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.dqman.model.DataIntegration;
import org.springframework.stereotype.Component;

/**
 * Processor to fetch metadata from a RDBMS.
 */
@Component
public class RdbmsMetadataProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        DataIntegration integration = exchange.getIn().getBody(DataIntegration.class);
        if (integration == null) {
            throw new IllegalArgumentException("Integration cannot be null");
        }

        List<String> metadata = new ArrayList<>();

        // Using standard JDBC here to handle dynamic credentials per request
        // without creating permanent DataSources in the registry
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(
                integration.getUrl(),
                integration.getUser(),
                integration.getPassword());
                java.sql.Statement stmt = conn.createStatement();
                java.sql.ResultSet rs = stmt.executeQuery(
                        "SELECT table_name, column_name, data_type, character_maximum_length FROM information_schema.columns "
                                +
                                "WHERE table_schema = 'public' " +
                                "ORDER BY table_name, ordinal_position")) {

            while (rs.next()) {
                String tableName = rs.getString("table_name");
                String columnName = rs.getString("column_name");
                String dataType = rs.getString("data_type");
                String maxLength = rs.getString("character_maximum_length");
                metadata.add(tableName + "." + columnName + " (" + dataType + ", " + maxLength + ")");
            }
        }
        exchange.getIn().setBody(metadata);
    }
}
