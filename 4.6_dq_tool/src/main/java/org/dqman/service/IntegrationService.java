package org.dqman.service;

import org.apache.camel.ProducerTemplate;
import org.dqman.model.DataIntegration;
import org.dqman.model.StepData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data integration service
 */
@Service
public class IntegrationService {

    @Autowired
    private ProducerTemplate producerTemplate; // for camel routes

    private Map<Long, List<String>> metadataCache = new HashMap<>();

    /**
     * Fetches metadata for a data integration
     * 
     * @param integration data integration
     * @return metadata
     */
    @SuppressWarnings("unchecked")
    public List<String> getMetadata(DataIntegration integration) {
        if (metadataCache.containsKey(integration.getId())) {
            return metadataCache.get(integration.getId());
        }
        List<String> metadata = switch (integration.getType()) {
            case "RDBMS" -> producerTemplate.requestBody("direct:fetchMetadataRdbms", integration, List.class);
            case "CSV" -> producerTemplate.requestBody("direct:fetchMetadataFile", integration, List.class);
            default -> null;
        };
        return metadata;
    }

    /**
     * Refreshes metadata for a data integration
     * 
     * @param integration data integration
     * @return metadata
     */
    public List<String> refreshMetadata(DataIntegration integration) {
        if (metadataCache.containsKey(integration.getId())) {
            metadataCache.remove(integration.getId());
        }
        return getMetadata(integration);
    }

    /**
     * Fetches data for a data integration
     * 
     * @param integration data integration
     * @param metadata    metadata
     * @return data
     */
    public StepData getData(DataIntegration integration, List<String> metadata) {
        StepData data = switch (integration.getType()) {
            case "RDBMS" -> producerTemplate.requestBodyAndHeader("direct:fetchDataRdbms", integration, "metadata",
                    metadata, StepData.class);
            case "CSV" -> producerTemplate.requestBody("direct:fetchDataFile", integration, StepData.class);
            default -> null;
        };
        return data;
    }

    /**
     * Writes data to a data integration
     * 
     * @param integration data integration
     * @param data        data
     * @return result
     */
    public StepData writeData(DataIntegration integration, StepData data) {
        StepData result = switch (integration.getType()) {
            case "RDBMS" -> producerTemplate.requestBodyAndHeader("direct:saveDataRdbms", integration, "data",
                    data, StepData.class);
            case "CSV" -> producerTemplate.requestBodyAndHeader("direct:writeDataFile", data, "integration",
                    integration, StepData.class);
            default -> null;
        };
        return result;
    }
}