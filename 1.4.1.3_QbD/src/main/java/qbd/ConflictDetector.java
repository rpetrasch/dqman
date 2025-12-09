package qbd;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class is used to detect conflicts between two aircraft.
 * 
 * ToDo implement
 */
public class ConflictDetector {

    private List<ConflictRule> rules; // conflict rules to use

    /**
     * create a new conflict detector
     * 
     * @param rules the list of rules to use
     */
    public ConflictDetector(List<ConflictRule> rules) {
        this.rules = rules;
    }

    /**
     * detect conflicts between two aircraft
     * 
     * @param aircraft1 the first aircraft
     * @param aircraft2 the second aircraft
     * @return the list of conflicts
     */
    public List<Conflict> detect(UnifiedAircraft aircraft1, UnifiedAircraft aircraft2) {
        return rules.stream()
                .map(rule -> rule.detect(aircraft1, aircraft2))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(c -> getRuleForConflict(c).priority()))
                .collect(Collectors.toList());
    }

    /**
     * get the rule for the given conflict
     * ToDo implement
     * 
     * @param conflict the conflict
     * @return the rule for the given conflict
     */
    private ConflictRule getRuleForConflict(Conflict conflict) {
        // Logic to identify which rule produced this conflict
        return rules.get(0); // Simplified
    }
}