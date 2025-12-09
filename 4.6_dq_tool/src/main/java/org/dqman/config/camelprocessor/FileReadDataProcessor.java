package org.dqman.config.camelprocessor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.dqman.model.DataIntegration;
import org.dqman.model.StepData;
import org.springframework.stereotype.Component;

/**
 * Processor to fetch data from a file.
 */
@Component
public class FileReadDataProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        DataIntegration integration = exchange.getIn().getBody(DataIntegration.class);
        if (integration == null) {
            throw new IllegalArgumentException("Integration cannot be null");
        }

        List<List<String>> data = new ArrayList<>();

        // Get data from CSV file
        Path file = Paths.get(integration.getUrl());
        Files.lines(file).forEach(line -> data.add(List.of(line.split(","))));

        StepData result = new StepData(integration.getUrl(), data);
        exchange.getIn().setBody(result);
    }
}
