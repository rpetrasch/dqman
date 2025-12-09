package org.dqman.controller;

import org.dqman.model.DataIntegration;
import org.dqman.model.StepData;
import org.dqman.repository.DataIntegrationRepository;
import org.dqman.service.IntegrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/integrations")
@CrossOrigin(origins = "http://localhost:4200")
public class DataIntegrationController {

    @Autowired
    private DataIntegrationRepository dataIntegrationRepository;
    @Autowired
    private IntegrationService integrationService;

    @GetMapping
    public List<DataIntegration> getAllIntegrations() {
        return dataIntegrationRepository.findAll();
    }

    @PostMapping
    public DataIntegration createIntegration(@RequestBody DataIntegration integration) {
        return dataIntegrationRepository.save(integration);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataIntegration> getIntegrationById(@PathVariable Long id) {
        return dataIntegrationRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<DataIntegration> updateIntegration(@PathVariable Long id,
            @RequestBody DataIntegration integrationDetails) {
        return dataIntegrationRepository.findById(id)
                .map(integration -> {
                    integration.setName(integrationDetails.getName());
                    integration.setDescription(integrationDetails.getDescription());
                    integration.setType(integrationDetails.getType());
                    integration.setUrl(integrationDetails.getUrl());
                    integration.setUser(integrationDetails.getUser());
                    integration.setPassword(integrationDetails.getPassword());
                    return ResponseEntity.ok(dataIntegrationRepository.save(integration));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIntegration(@PathVariable Long id) {
        if (dataIntegrationRepository.existsById(id)) {
            dataIntegrationRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/metadata/{id}")
    public ResponseEntity<List<String>> getIntegrationMetadata(@PathVariable Long id) {
        try {
            DataIntegration integration = dataIntegrationRepository.findById(id).orElse(null);
            if (integration == null) {
                return ResponseEntity.notFound().build();
            }
            List<String> metadata = integrationService.getMetadata(integration);
            if (metadata == null) {
                return ResponseEntity.status(500).body(List.of("No metadata returned"));
            }
            return ResponseEntity.ok(metadata);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(List.of("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/data/{id}")
    public ResponseEntity<StepData> getIntegrationData(@PathVariable Long id) {
        DataIntegration integration = dataIntegrationRepository.findById(id).orElse(null);
        if (integration == null) {
            return ResponseEntity.notFound().build();
        }
        List<String> metadata = integrationService.getMetadata(integration); // ToDo optional metadata parameter
        StepData data = integrationService.getData(integration, metadata);
        return ResponseEntity.ok(data);
    }
}
