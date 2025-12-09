package qbd;

/**
 * This class is used to implement the conflict rules.
 */
public class ConflictRuleImpl {

    /**
     * This class is used to implement the separation rule.
     */
    public static class SeparationRule implements ConflictRule {
        private static final int MIN_SEPARATION = 1000; // feet

        /**
         * detect conflicts between two aircraft
         * 
         * @param aircraft1 the first aircraft
         * @param aircraft2 the second aircraft
         * @return the conflict if any
         */
        @Override
        public Conflict detect(UnifiedAircraft aircraft1, UnifiedAircraft aircraft2) {
            double distance = calculateSeparation(aircraft1, aircraft2);
            if (distance < MIN_SEPARATION) {
                int timeToConflict = calculateTimeToConflict(aircraft1, aircraft2);
                return new Conflict(aircraft1, aircraft2, distance, timeToConflict, "Separation violation");
            }
            return null;
        }

        /**
         * get the priority of the rule
         * 
         * @return the priority of the rule
         */
        @Override
        public int priority() {
            return 1; // Highest priority
        }

        /**
         * get the name of the rule
         * 
         * @return the name of the rule
         */
        public String name() {
            return "Separation Rule";
        }

        /**
         * calculate the separation between two aircraft
         * 
         * @param a1 the first aircraft
         * @param a2 the second aircraft
         * @return the separation between the two aircraft
         */
        private double calculateSeparation(UnifiedAircraft a1, UnifiedAircraft a2) {
            // Calculate 3D distance between aircraft
            return Math.sqrt(
                    Math.pow(a1.getPosition().getLatitude() - a2.getPosition().getLatitude(), 2) +
                            Math.pow(a1.getPosition().getLongitude() - a2.getPosition().getLongitude(), 2) +
                            Math.pow(a1.getPosition().getAltitude() - a2.getPosition().getAltitude(), 2));
        }

        /**
         * calculate the time to conflict
         * 
         * @param a1 the first aircraft
         * @param a2 the second aircraft
         * @return the time to conflict
         */
        private int calculateTimeToConflict(UnifiedAircraft a1, UnifiedAircraft a2) {
            // Calculate time until separation falls below minimum
            return 0; // Simplified
        }
    }

    /**
     * This class is used to implement the weather conflict rule.
     */
    public static class WeatherConflictRule implements ConflictRule {

        /**
         * detect conflicts between two aircraft
         * 
         * @param aircraft1 the first aircraft
         * @param aircraft2 the second aircraft
         * @return the conflict if any
         */
        @Override
        public Conflict detect(UnifiedAircraft aircraft1, UnifiedAircraft aircraft2) {
            boolean aircraft1InHazard = !aircraft1.getActiveHazards().isEmpty();
            boolean aircraft2InHazard = !aircraft2.getActiveHazards().isEmpty();

            if (aircraft1InHazard && aircraft2InHazard) {
                Position pos1 = aircraft1.getPosition();
                Position pos2 = aircraft2.getPosition();

                if (pathsIntersect(pos1, pos2)) {
                    return new Conflict(aircraft1, aircraft2, 0, 0, "Paths intersect in weather hazard");
                }
            }
            return null;
        }

        /**
         * get the priority of the rule
         * 
         * @return the priority of the rule
         */
        @Override
        public int priority() {
            return 2;
        }

        /**
         * get the name of the rule
         * 
         * @return the name of the rule
         */
        public String name() {
            return "Weather Conflict Rule";
        }

        private boolean pathsIntersect(Position pos1, Position pos2) {
            // Simplified path intersection logic
            return false;
        }
    }

    /**
     * This class is used to implement the fuel emergency rule.
     */
    public static class FuelEmergencyRule implements ConflictRule {

        /**
         * detect conflicts between two aircraft
         * 
         * @param aircraft1 the first aircraft
         * @param aircraft2 the second aircraft
         * @return the conflict if any
         */
        @Override
        public Conflict detect(UnifiedAircraft aircraft1, UnifiedAircraft aircraft2) {
            boolean a1FuelEmergency = aircraft1.hasDeclaration("FUEL");
            boolean a2FuelEmergency = aircraft2.hasDeclaration("FUEL");

            if (a1FuelEmergency || a2FuelEmergency) {
                UnifiedAircraft emergencyAircraft = a1FuelEmergency ? aircraft1 : aircraft2;
                return new Conflict(aircraft1, aircraft2, 0, 0,
                        emergencyAircraft.getCallSign() + " declared fuel emergency");
            }
            return null;
        }

        @Override
        public int priority() {
            return 0; // Highest priority
        }

        @Override
        public String name() {
            return "Fuel Emergency Rule";
        }
    }
}