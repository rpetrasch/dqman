package org.dqman.config.camelprocessor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.dqman.model.DataIntegration;
import org.springframework.stereotype.Component;

/**
 * Processor to fetch metadata from a file.
 */
@Component
public class FileMetadataProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        DataIntegration integration = exchange.getIn().getBody(DataIntegration.class);
        if (integration == null) {
            throw new IllegalArgumentException("Integration cannot be null");
        }

        List<String> metadata = new ArrayList<>();

        // Get metadata from CSV file
        Path file = Paths.get(integration.getUrl());
        // Path file = Paths.get(System.getProperty("user.dir") + "/" +
        // integration.getUrl()); // for local file testing

        // Get header
        String header = Files.readAllLines(file).get(0);
        BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);

        metadata.add("header: " + header);
        metadata.add("creationTime: " + attr.creationTime());
        metadata.add("lastAccessTime: " + attr.lastAccessTime());
        metadata.add("lastModifiedTime: " + attr.lastModifiedTime());
        metadata.add("isDirectory: " + attr.isDirectory());
        metadata.add("isOther: " + attr.isOther());
        metadata.add("isRegularFile: " + attr.isRegularFile());
        metadata.add("isSymbolicLink: " + attr.isSymbolicLink());
        metadata.add("size: " + attr.size());

        exchange.getIn().setBody(metadata);
    }
}
