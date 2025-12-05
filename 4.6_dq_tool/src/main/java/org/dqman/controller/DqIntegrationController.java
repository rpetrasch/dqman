package org.dqman.controller;

import org.dqman.model.DqIntegration;
import org.dqman.repository.DqIntegrationRepository;
import org.dqman.service.IntegrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/integrations")
@CrossOrigin(origins = "http://localhost:4200")
public class DqIntegrationController {

    @Autowired
    private DqIntegrationRepository dqIntegrationRepository;
    @Autowired
    private IntegrationService integrationService;

    @GetMapping
    public List<DqIntegration> getAllIntegrations() {
        return dqIntegrationRepository.findAll();
    }

    @PostMapping
    public DqIntegration createIntegration(@RequestBody DqIntegration integration) {
        return dqIntegrationRepository.save(integration);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DqIntegration> getIntegrationById(@PathVariable Long id) {
        return dqIntegrationRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<DqIntegration> updateIntegration(@PathVariable Long id,
            @RequestBody DqIntegration integrationDetails) {
        return dqIntegrationRepository.findById(id)
                .map(integration -> {
                    integration.setName(integrationDetails.getName());
                    integration.setDescription(integrationDetails.getDescription());
                    integration.setType(integrationDetails.getType());
                    integration.setUrl(integrationDetails.getUrl());
                    integration.setUser(integrationDetails.getUser());
                    integration.setPassword(integrationDetails.getPassword());
                    return ResponseEntity.ok(dqIntegrationRepository.save(integration));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIntegration(@PathVariable Long id) {
        if (dqIntegrationRepository.existsById(id)) {
            dqIntegrationRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/metadata/{id}")
    public ResponseEntity<List<String>> getIntegrationMetadata(@PathVariable Long id) {
        try {
            DqIntegration integration = dqIntegrationRepository.findById(id).orElse(null);
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
    public ResponseEntity<Map<String, List<List<String>>>> getIntegrationData(@PathVariable Long id) {
        DqIntegration integration = dqIntegrationRepository.findById(id).orElse(null);
        if (integration == null) {
            return ResponseEntity.notFound().build();
        }
        List<String> metadata = integrationService.getMetadata(integration); // ToDo optional metadata parameter
        Map<String, List<List<String>>> data = integrationService.getData(integration, metadata);
        return ResponseEntity.ok(data);
    }
}
