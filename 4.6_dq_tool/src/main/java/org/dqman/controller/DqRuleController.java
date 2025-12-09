package org.dqman.controller;

import org.dqman.model.DqRule;
import org.dqman.repository.DqRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
@CrossOrigin(origins = "http://localhost:4200")
public class DqRuleController {

    @Autowired
    private DqRuleRepository dqRuleRepository;

    @GetMapping
    public List<DqRule> getAllRules() {
        return dqRuleRepository.findAll();
    }

    @PostMapping
    public DqRule createRule(@RequestBody DqRule rule) {
        return dqRuleRepository.save(rule);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DqRule> getRuleById(@PathVariable Long id) {
        return dqRuleRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<DqRule> updateRule(@PathVariable Long id, @RequestBody DqRule ruleDetails) {
        return dqRuleRepository.findById(id)
                .map(rule -> {
                    rule.setName(ruleDetails.getName());
                    rule.setDescription(ruleDetails.getDescription());
                    rule.setRuleType(ruleDetails.getRuleType());
                    rule.setRuleValue(ruleDetails.getRuleValue());
                    rule.setSourceTableFieldList(ruleDetails.getSourceTableFieldList());
                    return ResponseEntity.ok(dqRuleRepository.save(rule));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        if (dqRuleRepository.existsById(id)) {
            dqRuleRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
