package org.dqman.nifi;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.Validator;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Custom NiFi Processor that transforms CSV data to uppercase
 * and adds a timestamp header
 */
public class TransformDataProcessor extends AbstractProcessor {

    // Define property descriptors
    public static final PropertyDescriptor PREFIX_PROPERTY = new PropertyDescriptor.Builder()
            .name("Text Prefix")
            .description("Prefix to add to each line")
            .required(false)
            .defaultValue("")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor UPPERCASE_PROPERTY = new PropertyDescriptor.Builder()
            .name("Convert to Uppercase")
            .description("Convert text to uppercase")
            .required(true)
            .defaultValue("true")
            .allowableValues("true", "false")
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();

    // Define relationships
    public static final Relationship SUCCESS = new Relationship.Builder()
            .name("success")
            .description("Successfully processed FlowFiles")
            .build();

    public static final Relationship FAILURE = new Relationship.Builder()
            .name("failure")
            .description("FlowFiles that failed processing")
            .build();

    private final List<PropertyDescriptor> properties = Collections.unmodifiableList(
            new ArrayList<PropertyDescriptor>() {{
                add(PREFIX_PROPERTY);
                add(UPPERCASE_PROPERTY);
            }}
    );

    private final Set<Relationship> relationships = Collections.unmodifiableSet(
            new HashSet<Relationship>() {{
                add(SUCCESS);
                add(FAILURE);
            }}
    );

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return this.properties;
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
        FlowFile flowFile = session.get();

        if (flowFile == null) {
            return;
        }

        try {
            // Get property values
            final String prefix = context.getProperty(PREFIX_PROPERTY).getValue();
            final boolean toUppercase = context.getProperty(UPPERCASE_PROPERTY).asBoolean();

            // Read and transform the FlowFile content
            flowFile = session.write(flowFile, (in, out) -> {
                try {
                    String content = readInputStream(in);
                    String transformed = transformContent(content, prefix, toUppercase);
                    out.write(transformed.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    throw new ProcessException(e);
                }
            });

            // Add attributes to the FlowFile
            flowFile = session.putAttribute(flowFile, "processed.timestamp",
                    String.valueOf(System.currentTimeMillis()));
            flowFile = session.putAttribute(flowFile, "processed.by", "TransformDataProcessor");

            // Route to success relationship
            session.transfer(flowFile, SUCCESS);

            // Log successful processing
            getLogger().info("Successfully processed FlowFile: " + flowFile.getId());

        } catch (Exception e) {
            getLogger().error("Error processing FlowFile", e);
            session.transfer(flowFile, FAILURE);
        }
    }

    /**
     * Reads content from InputStream
     */
    private String readInputStream(InputStream in) throws IOException {
        byte[] buffer = new byte[1024];
        StringBuilder sb = new StringBuilder();
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    /**
     * Transforms content based on properties
     */
    private String transformContent(String content, String prefix, boolean toUppercase) {
        StringBuilder result = new StringBuilder();
        result.append("# Processed: ").append(System.currentTimeMillis()).append("\n");

        String[] lines = content.split("\n");
        for (String line : lines) {
            String transformed = line;

            if (toUppercase) {
                transformed = transformed.toUpperCase();
            }

            if (prefix != null && !prefix.isEmpty()) {
                transformed = prefix + transformed;
            }

            result.append(transformed).append("\n");
        }

        return result.toString();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}