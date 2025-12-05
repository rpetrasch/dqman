package org.dqman.controller;

import org.dqman.model.DqFlow;
import org.dqman.model.DqFlowStep;
import org.dqman.model.DqIntegration;
import org.dqman.model.DqRule;
import org.dqman.repository.DqFlowRepository;
import org.dqman.repository.DqIntegrationRepository;
import org.dqman.repository.DqRuleRepository;
import org.dqman.service.FlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/flows")
@CrossOrigin(origins = "http://localhost:4200")
public class DqFlowController {

    @Autowired
    private FlowService flowService;

    @Autowired
    private DqFlowRepository dqFlowRepository;

    @Autowired
    private DqIntegrationRepository dqIntegrationRepository;

    @Autowired
    private DqRuleRepository dqRuleRepository;

    @GetMapping
    public List<DqFlow> getAllFlows() {
        return dqFlowRepository.findAll();
    }

    @PostMapping
    public DqFlow createFlow(@RequestBody JsonNode flowJson) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        DqFlow flow = mapper.convertValue(flowJson, DqFlow.class);

        if (flow.getCreatedDate() == null) {
            flow.setCreatedDate(ZonedDateTime.now());
        }
        flow.setModifiedDate(ZonedDateTime.now());

        // Ensure steps carry the reference to this flow and resolve integration/rule
        // references
        if (flow.getSteps() != null) {
            JsonNode stepsNode = flowJson.get("steps");
            for (int i = 0; i < flow.getSteps().size(); i++) {
                DqFlowStep step = flow.getSteps().get(i);
                step.setFlow(flow);

                // Handle integrationId
                if (stepsNode != null && stepsNode.has(i)) {
                    JsonNode stepNode = stepsNode.get(i);
                    if (stepNode.has("integrationId") && !stepNode.get("integrationId").isNull()) {
                        Long integrationId = stepNode.get("integrationId").asLong();
                        DqIntegration integration = dqIntegrationRepository.findById(integrationId).orElse(null);
                        step.setIntegration(integration);
                    }

                    // Handle ruleId
                    if (stepNode.has("ruleId") && !stepNode.get("ruleId").isNull()) {
                        Long ruleId = stepNode.get("ruleId").asLong();
                        DqRule rule = dqRuleRepository.findById(ruleId).orElse(null);
                        step.setRule(rule);
                    }
                }
            }
        }

        return dqFlowRepository.save(flow);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DqFlow> getFlowById(@PathVariable Long id) {
        return dqFlowRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<DqFlow> updateFlow(@PathVariable Long id, @RequestBody JsonNode flowJson) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        DqFlow flowDetails = mapper.convertValue(flowJson, DqFlow.class);

        return dqFlowRepository.findById(id)
                .map(flow -> {
                    flow.setName(flowDetails.getName());
                    flow.setDescription(flowDetails.getDescription());
                    flow.setStatus(flowDetails.getStatus());
                    flow.setModifiedDate(ZonedDateTime.now());

                    // Manage steps manually if needed, or let CascadeType.ALL handle common cases.
                    // A simple replacement strategy for List<Steps> if passed fully:
                    flow.getSteps().clear();
                    if (flowDetails.getSteps() != null) {
                        JsonNode stepsNode = flowJson.get("steps");
                        for (int i = 0; i < flowDetails.getSteps().size(); i++) {
                            DqFlowStep step = flowDetails.getSteps().get(i);

                            if (stepsNode != null && stepsNode.has(i)) {
                                JsonNode stepNode = stepsNode.get(i);
                                // Handle integrationId
                                if (stepNode.has("integrationId") && !stepNode.get("integrationId").isNull()) {
                                    Long integrationId = stepNode.get("integrationId").asLong();
                                    DqIntegration integration = dqIntegrationRepository.findById(integrationId)
                                            .orElse(null);
                                    step.setIntegration(integration);
                                }

                                // Handle ruleId
                                if (stepNode.has("ruleId") && !stepNode.get("ruleId").isNull()) {
                                    Long ruleId = stepNode.get("ruleId").asLong();
                                    DqRule rule = dqRuleRepository.findById(ruleId).orElse(null);
                                    step.setRule(rule);
                                }
                            }

                            flow.addStep(step);
                        }
                    }
                    DqFlow savedFlow = dqFlowRepository.save(flow);
                    return ResponseEntity.ok(savedFlow);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<Map<String, Object>> executeFlow(@PathVariable Long id) {
        try {
            Map<String, Object> result = flowService.executeFlow(id);

            List<Map<String, Object>> stepResults = (List<Map<String, Object>>) result.get("steps");
            result.put("endTime", ZonedDateTime.now().toString());
            result.put("totalSteps", stepResults.size());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "ERROR");
            errorResult.put("message", "Flow execution failed: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResult);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFlow(@PathVariable Long id) {
        if (dqFlowRepository.existsById(id)) {
            dqFlowRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
