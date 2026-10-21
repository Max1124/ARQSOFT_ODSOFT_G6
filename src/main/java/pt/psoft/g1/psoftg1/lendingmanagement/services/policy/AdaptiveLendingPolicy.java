package pt.psoft.g1.psoftg1.lendingmanagement.services.policy;

/** Adaptive Lending Policy, specification section 3 (LP-A1 to LP-A5): the period depends on the reader's active lendings. */
public class AdaptiveLendingPolicy implements LendingPolicy {
    private final AdaptivePolicyParameters parameters;

    // Parameters are injected so thresholds and periods can be fixed in tests (QAS11, CON-LP-4).
    public AdaptiveLendingPolicy(AdaptivePolicyParameters parameters) {
        this.parameters = parameters;
    }

    @Override
    public PolicyDecision evaluateNewLending(ReaderLendingHistory history) {
        if (history.overdueLendings() > 0) {
            return PolicyDecision.reject(RejectionReason.OVERDUE_LENDING);
        }
        if (history.activeLendings() >= parameters.maxActiveLendings()) {
            return PolicyDecision.reject(RejectionReason.MAX_ACTIVE_LENDINGS);
        }
        // Count before the new lending is added: the period reflects the reader's load at request time (design decision).
        return PolicyDecision.allow(periodFor(history.activeLendings()));
    }

    @Override
    public PolicyDecision evaluateRenewal(LendingToRenew lending, ReaderLendingHistory history) {
        // The specification only says a renewal must keep satisfying the policy; the group decided overdue lendings are refused (3.6).
        if (lending.overdue()) {
            return PolicyDecision.reject(RejectionReason.LENDING_OVERDUE);
        }
        // No renewal count here: the specification defines no Adaptive renewal limit (design decision).
        return PolicyDecision.allow(periodFor(history.activeLendings()));
    }

    // Thresholds are lower-inclusive: exactly FirstThreshold already gets the standard period (LP-A2).
    private int periodFor(int activeLendings) {
        if (activeLendings < parameters.firstThreshold()) {
            return parameters.longPeriodDays();
        }
        if (activeLendings < parameters.secondThreshold()) {
            return parameters.standardPeriodDays();
        }
        return parameters.shortPeriodDays();
    }
}
