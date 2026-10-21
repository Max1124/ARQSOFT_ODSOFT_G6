package pt.psoft.g1.psoftg1.lendingmanagement.services.policy;

/**
 * Common contract of every lending policy (Strategy). LendingServiceImpl depends only on this,
 * so a new policy is a new class and the service does not change (QAS3, EXT-LP-01).
 */
public interface LendingPolicy {

    PolicyDecision evaluateNewLending(ReaderLendingHistory history);

    PolicyDecision evaluateRenewal(LendingToRenew lending, ReaderLendingHistory history);
}
