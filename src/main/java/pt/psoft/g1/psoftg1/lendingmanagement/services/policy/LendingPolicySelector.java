package pt.psoft.g1.psoftg1.lendingmanagement.services.policy;

import java.util.List;
import java.util.Optional;

/** Returns the policy that applies to a reader: the first matching rule's policy, otherwise Standard (LP-SEL1). */
public class LendingPolicySelector {
    private final List<PolicySelectionRule> rules;
    private final LendingPolicy defaultPolicy;

    // Rules are an ordered copy: the first match wins, so order is part of the configuration and cannot change afterwards.
    public LendingPolicySelector(List<PolicySelectionRule> rules, LendingPolicy defaultPolicy) {
        this.rules = List.copyOf(rules);
        this.defaultPolicy = defaultPolicy;
    }

    // Called on every new lending and renewal and never cached, so the choice follows the reader's current history (LP-SEL2).
    public LendingPolicy select(ReaderLendingHistory history) {
        for (PolicySelectionRule rule : rules) {
            Optional<LendingPolicy> policy = rule.select(history);
            if (policy.isPresent()) {
                return policy.get();
            }
        }
        return defaultPolicy;
    }
}
