public interface ConflictRule {
    Conflict detect(UnifiedAircraft aircraft1, UnifiedAircraft aircraft2);
    int priority();
    String name();
}