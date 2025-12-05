package org.dqman.processor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.dqman.model.DqIntegration;
import org.springframework.stereotype.Component;

/**
 * Processor to fetch data from a file.
 */
@Component
public class FileDataProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        DqIntegration integration = exchange.getIn().getBody(DqIntegration.class);
        if (integration == null) {
            throw new IllegalArgumentException("Integration cannot be null");
        }

        List<List<String>> data = new ArrayList<>();

        // Get data from CSV file
        Path file = Paths.get(integration.getUrl());
        Files.lines(file).forEach(line -> data.add(List.of(line.split(","))));

        Map<String, List<List<String>>> result = new HashMap<>();
        result.put(integration.getUrl(), data);

        exchange.getIn().setBody(result);
    }
}
