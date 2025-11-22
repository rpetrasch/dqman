import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<ConflictRule> rules = Arrays.asList(
                new ConflictRuleImpl.FuelEmergencyRule(),
                new ConflictRuleImpl.SeparationRule(),
                new ConflictRuleImpl.WeatherConflictRule()
        );
        ConflictDetector detector = new ConflictDetector(rules);
        UnifiedAircraft aircraft1 = new UnifiedAircraft();
        UnifiedAircraft aircraft2 = new UnifiedAircraft();
        List<Conflict> conflicts = detector.detect(aircraft1, aircraft2);
    }
}