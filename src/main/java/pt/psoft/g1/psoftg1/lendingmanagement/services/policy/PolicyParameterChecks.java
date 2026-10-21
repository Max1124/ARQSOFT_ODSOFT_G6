package pt.psoft.g1.psoftg1.lendingmanagement.services.policy;

/** Validation helpers shared by the lending policy parameter objects; messages name the parameter and spec clause. */
final class PolicyParameterChecks {
    private PolicyParameterChecks() {}

    static void positive(String name, long value, String clause) {
        if (value <= 0) {
            throw invalid(name, value, "must be > 0", clause);
        }
    }

    static void notNegative(String name, long value, String clause) {
        if (value < 0) {
            throw invalid(name, value, "must be >= 0", clause);
        }
    }

    static void require(boolean condition, String name, Object value, String constraint, String clause) {
        if (!condition) {
            throw invalid(name, value, constraint, clause);
        }
    }

    private static IllegalArgumentException invalid(String name, Object value, String constraint, String clause) {
        return new IllegalArgumentException(
                "Invalid lending parameter " + name + "=" + value + ": " + constraint + " (specification " + clause + ")");
    }
}
