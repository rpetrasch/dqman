import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ConflictDetector {
    private List<ConflictRule> rules;

    public ConflictDetector(List<ConflictRule> rules) {
        this.rules = rules;
    }

    public List<Conflict> detect(UnifiedAircraft aircraft1, UnifiedAircraft aircraft2) {
        return rules.stream()
                .map(rule -> rule.detect(aircraft1, aircraft2))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(c -> getRuleForConflict(c).priority()))
                .collect(Collectors.toList());
    }

    private ConflictRule getRuleForConflict(Conflict conflict) {
        // Logic to identify which rule produced this conflict
        return rules.get(0); // Simplified
    }
}