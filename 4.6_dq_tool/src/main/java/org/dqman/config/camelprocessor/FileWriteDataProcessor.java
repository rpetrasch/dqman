package org.dqman.config.camelprocessor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.dqman.model.DataIntegration;
import org.dqman.model.StepData;
import org.springframework.stereotype.Component;

/**
 * Processor to write data to a file.
 */
@Component
public class FileWriteDataProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        DataIntegration integration = exchange.getIn().getHeader("integration", DataIntegration.class);
        if (integration == null) {
            throw new IllegalArgumentException("DataIntegration cannot be null");
        }

        // Get the StepData from the exchange
        StepData stepData = exchange.getIn().getBody(StepData.class);
        if (stepData == null) {
            throw new IllegalArgumentException("StepData cannot be null");
        }

        // StepData is a record with a Map<String, List<List<String>>>
        // The key is the file path, the value is the data
        if (stepData.data() == null || stepData.data().isEmpty()) {
            throw new IllegalArgumentException("StepData must contain data");
        }

        // Get the file path
        Path file = Paths.get(integration.getUrl());

        // CSV data array
        List<String> csvLines = new ArrayList<>();

        // Get the entries
        for (Map.Entry<String, List<List<String>>> entry : stepData.data().entrySet()) {
            String header = entry.getKey();
            csvLines.add(header);
            List<List<String>> data = entry.getValue();

            // Convert List<List<String>> to CSV format
            for (List<String> row : data) {
                csvLines.add(String.join(",", row));
            }
        }

        // Write data to file
        Files.write(file, csvLines);

        // Keep the StepData in the exchange for downstream processing
        exchange.getIn().setBody(stepData);
    }
}
