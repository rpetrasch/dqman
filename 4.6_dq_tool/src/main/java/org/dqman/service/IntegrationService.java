package org.dqman.service;

import org.apache.camel.ProducerTemplate;
import org.dqman.model.DqIntegration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class IntegrationService {

    @Autowired
    private ProducerTemplate producerTemplate;

    @SuppressWarnings("unchecked")
    public List<String> getMetadata(DqIntegration integration) {
        List<String> metadata = switch (integration.getType()) {
            case "RDBMS" -> producerTemplate.requestBody("direct:fetchMetadataRdbms", integration, List.class);
            case "CSV" -> producerTemplate.requestBody("direct:fetchMetadataFile", integration, List.class);
            default -> null;
        };
        return metadata;
    }

    public Map<String, List<List<String>>> getData(DqIntegration integration, List<String> metadata4) {
        Map<String, List<List<String>>> data = switch (integration.getType()) {
            case "RDBMS" -> producerTemplate.requestBodyAndHeader("direct:fetchDataRdbms", integration, "metadata",
                    metadata4, Map.class);
            case "CSV" -> producerTemplate.requestBody("direct:fetchDataFile", integration, Map.class);
            default -> null;
        };
        return data;
    }
}