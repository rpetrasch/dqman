package org.dqman.controller;

import org.dqman.model.DqFlow;
import org.dqman.model.DqFlowStep;
import org.dqman.model.DataIntegration;
import org.dqman.model.DqRule;
import org.dqman.repository.DqFlowRepository;
import org.dqman.repository.DataIntegrationRepository;
import org.dqman.repository.DqRuleRepository;
import org.dqman.service.FlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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
    private DataIntegrationRepository dataIntegrationRepository;

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
                        DataIntegration integration = dataIntegrationRepository.findById(integrationId).orElse(null);
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
    @Transactional
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

                        // First pass: Add all steps
                        for (int i = 0; i < flowDetails.getSteps().size(); i++) {
                            DqFlowStep step = flowDetails.getSteps().get(i);

                            if (stepsNode != null && stepsNode.has(i)) {
                                JsonNode stepNode = stepsNode.get(i);
                                // Handle integrationId
                                if (stepNode.has("integrationId") && !stepNode.get("integrationId").isNull()) {
                                    Long integrationId = stepNode.get("integrationId").asLong();
                                    DataIntegration integration = dataIntegrationRepository.findById(integrationId)
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

                        // Save to get IDs for new steps
                        DqFlow savedFlow = dqFlowRepository.save(flow);
                        dqFlowRepository.flush(); // Force write to DB

                        System.out.println("=== Syncing relationships ===");

                        // Second pass: Sync successor/predecessor relationships from IDs
                        if (stepsNode != null) {
                            for (int i = 0; i < savedFlow.getSteps().size(); i++) {
                                DqFlowStep step = savedFlow.getSteps().get(i);
                                JsonNode stepNode = stepsNode.get(i);

                                System.out.println(
                                        "Processing step " + i + ": " + step.getName() + " (ID: " + step.getId() + ")");

                                // Clear existing relationships
                                step.getSuccessors().clear();
                                step.getPredecessors().clear();

                                // Handle successorIds
                                if (stepNode.has("successorIds") && stepNode.get("successorIds").isArray()) {
                                    JsonNode successorIdsNode = stepNode.get("successorIds");
                                    System.out.println("  Found " + successorIdsNode.size() + " successor IDs");

                                    for (JsonNode successorIdNode : successorIdsNode) {
                                        Long successorId = successorIdNode.asLong();
                                        System.out.println("    Looking for successor ID: " + successorId);

                                        // Find the successor step in the saved flow
                                        savedFlow.getSteps().stream()
                                                .filter(s -> s.getId() != null && s.getId().equals(successorId))
                                                .findFirst()
                                                .ifPresentOrElse(
                                                        successor -> {
                                                            step.addSuccessor(successor);
                                                            System.out.println("    Added successor: " + step.getName()
                                                                    + " -> " + successor.getName());
                                                        },
                                                        () -> System.out.println("    WARNING: Successor ID "
                                                                + successorId + " not found!"));
                                    }
                                }
                                // Handle predecessorIds
                                if (stepNode.has("predecessorIds") && stepNode.get("predecessorIds").isArray()) {
                                    JsonNode predecessorIdsNode = stepNode.get("predecessorIds");
                                    System.out.println("  Found " + predecessorIdsNode.size() + " predecessor IDs");

                                    for (JsonNode predecessorIdNode : predecessorIdsNode) {
                                        Long predecessorId = predecessorIdNode.asLong();
                                        System.out.println("    Looking for predecessor ID: " + predecessorId);

                                        // Find the predecessor step in the saved flow
                                        savedFlow.getSteps().stream()
                                                .filter(s -> s.getId() != null && s.getId().equals(predecessorId))
                                                .findFirst()
                                                .ifPresentOrElse(
                                                        predecessor -> {
                                                            step.addPredecessor(predecessor);
                                                            System.out
                                                                    .println("    Added predecessor: " + step.getName()
                                                                            + " -> " + predecessor.getName());
                                                        },
                                                        () -> System.out.println("    WARNING: Predecessor ID "
                                                                + predecessorId + " not found!"));
                                    }
                                }
                            }
                        }

                        System.out.println("=== Final save with relationships ===");
                        // Final save with relationships
                        savedFlow = dqFlowRepository.save(savedFlow);
                        dqFlowRepository.flush(); // Force write to DB

                        System.out.println("=== Relationships saved ===");
                        return ResponseEntity.ok(savedFlow);
                    }

                    DqFlow savedFlow = dqFlowRepository.save(flow);
                    return ResponseEntity.ok(savedFlow);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/validate")
    public ResponseEntity<Map<String, Object>> validateFlow(@PathVariable Long id) {
        Optional<DqFlow> flowOpt = dqFlowRepository.findById(id);
        if (!flowOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        DqFlow flow = flowOpt.get();
        List<String> errors = new ArrayList<>();

        if (flow.getSteps() == null || flow.getSteps().isEmpty()) {
            errors.add("Flow must have at least one step");
        } else {
            // Check each step
            for (DqFlowStep step : flow.getSteps()) {
                // Check for missing integrations
                if (("DATA SOURCE".equals(step.getType()) || "DATA SINK".equals(step.getType()))
                        && step.getIntegration() == null) {
                    errors.add("Step \"" + step.getName()
                            + "\" is a Data Source or Data Sink but has no integration selected");
                }

                // Check for missing rules
                if ("DQ RULE".equals(step.getType()) && step.getRule() == null) {
                    errors.add("Step \"" + step.getName() + "\" is a DQ Rule but has no rule selected");
                }

                // Check for missing connections
                boolean isInitial = step.getIsInitial() != null ? step.getIsInitial() : false;
                boolean isFinal = step.getIsFinal() != null ? step.getIsFinal() : false;
                boolean hasInputs = step.getPredecessors() != null && !step.getPredecessors().isEmpty();
                boolean hasOutputs = step.getSuccessors() != null && !step.getSuccessors().isEmpty();

                if (!isInitial && !hasInputs) {
                    errors.add("Step \"" + step.getName() + "\" is not marked as initial but has no input connections");
                }

                if (!isFinal && !hasOutputs) {
                    errors.add("Step \"" + step.getName() + "\" is not marked as final but has no output connections");
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("valid", errors.isEmpty());
        result.put("errors", errors);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<Map<String, Object>> executeFlow(@PathVariable Long id) {
        try {
            Map<String, Object> result = flowService.executeFlow(id);

            List<Map<String, Object>> stepResults = (List<Map<String, Object>>) result.get("steps");
            result.put("endTime", ZonedDateTime.now().toString());
            result.put("totalSteps", stepResults != null ? stepResults.size() : 0);

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
