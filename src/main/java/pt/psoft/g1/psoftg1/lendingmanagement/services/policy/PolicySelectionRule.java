package pt.psoft.g1.psoftg1.lendingmanagement.services.policy;

import java.util.Optional;

/**
 * Decides whether a reader qualifies for one policy. Kept apart from the policies so
 * "what a policy allows" and "who gets it" change independently (EXT-LP-03, QAS3, QAS8).
 */
@FunctionalInterface
public interface PolicySelectionRule {

    Optional<LendingPolicy> select(ReaderLendingHistory history);
}
