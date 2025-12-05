package org.dqman.controller;

import org.dqman.model.DqFlow;
import org.dqman.model.DqFlowStep;
import org.dqman.repository.DqFlowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/flows")
@CrossOrigin(origins = "http://localhost:4200")
public class DqFlowController {

    @Autowired
    private DqFlowRepository dqFlowRepository;

    @GetMapping
    public List<DqFlow> getAllFlows() {
        return dqFlowRepository.findAll();
    }

    @PostMapping
    public DqFlow createFlow(@RequestBody DqFlow flow) {
        if (flow.getCreatedDate() == null) {
            flow.setCreatedDate(ZonedDateTime.now());
        }
        flow.setModifiedDate(ZonedDateTime.now());

        // Ensure steps carry the reference to this flow
        if (flow.getSteps() != null) {
            for (DqFlowStep step : flow.getSteps()) {
                step.setFlow(flow);
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
    public ResponseEntity<DqFlow> updateFlow(@PathVariable Long id, @RequestBody DqFlow flowDetails) {
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
                        for (DqFlowStep step : flowDetails.getSteps()) {
                            flow.addStep(step);
                        }
                    }

                    return ResponseEntity.ok(dqFlowRepository.save(flow));
                })
                .orElse(ResponseEntity.notFound().build());
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
