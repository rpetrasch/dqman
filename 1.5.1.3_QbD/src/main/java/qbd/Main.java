package qbd;

import java.util.Arrays;
import java.util.List;

/**
 * This class is used to demonstrate the use of the ConflictDetector class.
 */
public class Main {

    /**
     * Test the conflict detection
     * ToDo implement, e.g. getPosition (currently null)
     * 
     * @param args not used
     */
    public static void main(String[] args) {
        // define the rules
        List<ConflictRule> rules = Arrays.asList(
                new ConflictRuleImpl.FuelEmergencyRule(),
                new ConflictRuleImpl.SeparationRule(),
                new ConflictRuleImpl.WeatherConflictRule());
        // create the detector
        ConflictDetector detector = new ConflictDetector(rules);
        // create the aircraft
        UnifiedAircraft aircraft1 = new UnifiedAircraft();
        UnifiedAircraft aircraft2 = new UnifiedAircraft();
        // detect the conflicts
        List<Conflict> conflicts = detector.detect(aircraft1, aircraft2);
        // print the conflicts
        for (Conflict conflict : conflicts) {
            System.out.println(conflict);
        }
    }
}