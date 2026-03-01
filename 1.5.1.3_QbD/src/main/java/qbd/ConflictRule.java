package qbd;

/**
 * This interface is used to represent a conflict rule.
 */
public interface ConflictRule {
    Conflict detect(UnifiedAircraft aircraft1, UnifiedAircraft aircraft2);

    int priority();

    String name();
}