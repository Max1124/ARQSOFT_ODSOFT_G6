package pt.psoft.g1.psoftg1.lendingmanagement.services.policy;

import java.util.Optional;

/** LP-SEL1 (specification 4.2): the reader qualifies for Adaptive when all three conditions hold. */
public class AdaptiveEligibilityRule implements PolicySelectionRule {
    private final PolicySelectionParameters parameters;
    private final LendingPolicy adaptivePolicy;

    // The policy is injected so the rule does not depend on a concrete class (open for new policies, QAS3).
    public AdaptiveEligibilityRule(PolicySelectionParameters parameters, LendingPolicy adaptivePolicy) {
        this.parameters = parameters;
        this.adaptivePolicy = adaptivePolicy;
    }

    @Override
    public Optional<LendingPolicy> select(ReaderLendingHistory history) {
        // 4.2.3: a currently overdue lending disqualifies the reader whatever the history says.
        if (history.overdueLendings() > 0) {
            return Optional.empty();
        }
        // 4.2.1: "at least" the minimum, so exactly MinCompletedLendings qualifies.
        if (history.completedInWindow() < parameters.minCompletedLendings()) {
            return Optional.empty();
        }
        // 4.2.2: compared as onTime * 100 >= ratio * completed to avoid dividing; completed > 0 here since the minimum is > 0.
        if (history.onTimeInWindow() * 100.0 < parameters.minOnTimeRatioPercent() * history.completedInWindow()) {
            return Optional.empty();
        }
        return Optional.of(adaptivePolicy);
    }
}
