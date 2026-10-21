package pt.psoft.g1.psoftg1.lendingmanagement.services.policy;

/** Outcome of a policy: allowed with a period in days, or rejected with a reason (EXT-LP-01). */
public record PolicyDecision(boolean allowed, int periodDays, RejectionReason reason) {

    // A structured result lets tests assert the decision instead of matching exception messages (observability, QAS11).
    public PolicyDecision {
        if (allowed && (periodDays <= 0 || reason != null)) {
            throw new IllegalArgumentException("An allowed decision needs a positive period and no reason");
        }
        if (!allowed && (periodDays != 0 || reason == null)) {
            throw new IllegalArgumentException("A rejected decision needs a reason and no period");
        }
    }

    public static PolicyDecision allow(int periodDays) {
        return new PolicyDecision(true, periodDays, null);
    }

    public static PolicyDecision reject(RejectionReason reason) {
        return new PolicyDecision(false, 0, reason);
    }
}
