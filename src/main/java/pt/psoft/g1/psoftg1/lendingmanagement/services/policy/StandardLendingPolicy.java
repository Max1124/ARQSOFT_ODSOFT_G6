package pt.psoft.g1.psoftg1.lendingmanagement.services.policy;

/** Standard Lending Policy, specification section 2 (LP-S1 to LP-S5). */
public class StandardLendingPolicy implements LendingPolicy {
    private final StandardPolicyParameters parameters;

    // Parameters are injected, never read from properties here, so the rules run without Spring (CON-LP-4).
    public StandardLendingPolicy(StandardPolicyParameters parameters) {
        this.parameters = parameters;
    }

    @Override
    public PolicyDecision evaluateNewLending(ReaderLendingHistory history) {
        // Overdue is checked before the limit, as the old service did, so the reader gets the more actionable reason (LP-S4).
        if (history.overdueLendings() > 0) {
            return PolicyDecision.reject(RejectionReason.OVERDUE_LENDING);
        }
        // The new lending would be one more than the active count, hence >= (LP-S1).
        if (history.activeLendings() >= parameters.maxActiveLendings()) {
            return PolicyDecision.reject(RejectionReason.MAX_ACTIVE_LENDINGS);
        }
        return PolicyDecision.allow(parameters.lendingPeriodDays());
    }

    @Override
    public PolicyDecision evaluateRenewal(LendingToRenew lending, ReaderLendingHistory history) {
        if (lending.overdue()) {
            return PolicyDecision.reject(RejectionReason.LENDING_OVERDUE);
        }
        // Only Standard counts renewals: the specification gives Adaptive no renewal limit (LP-S3).
        if (lending.renewalsMade() >= parameters.maxRenewals()) {
            return PolicyDecision.reject(RejectionReason.MAX_RENEWALS);
        }
        return PolicyDecision.allow(parameters.renewalPeriodDays());
    }
}
