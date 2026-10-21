package pt.psoft.g1.psoftg1.lendingmanagement.services.policy;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AdaptiveLendingPolicyTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);
    // max 5 active; below 2 -> 21 days, 2 and 3 -> 15 days, 4 or more -> 7 days
    private final AdaptiveLendingPolicy policy = new AdaptiveLendingPolicy(new AdaptivePolicyParameters(5, 2, 4, 21, 15, 7));

    private static ReaderLendingHistory history(int active, int overdue) {
        return new ReaderLendingHistory(TODAY, active, overdue, 0, 0);
    }

    @Test
    void longPeriodBelowTheFirstThreshold() {
        assertThat(policy.evaluateNewLending(history(0, 0))).isEqualTo(PolicyDecision.allow(21));
        assertThat(policy.evaluateNewLending(history(1, 0))).isEqualTo(PolicyDecision.allow(21));
    }

    @Test
    void standardPeriodFromTheFirstThresholdInclusive() {
        assertThat(policy.evaluateNewLending(history(2, 0))).isEqualTo(PolicyDecision.allow(15));
        assertThat(policy.evaluateNewLending(history(3, 0))).isEqualTo(PolicyDecision.allow(15));
    }

    @Test
    void shortPeriodFromTheSecondThresholdInclusive() {
        assertThat(policy.evaluateNewLending(history(4, 0))).isEqualTo(PolicyDecision.allow(7));
    }

    @Test
    void rejectsNewLendingWhenTheLimitIsReached() {
        assertThat(policy.evaluateNewLending(history(5, 0))).isEqualTo(PolicyDecision.reject(RejectionReason.MAX_ACTIVE_LENDINGS));
    }

    @Test
    void rejectsNewLendingWhenAnyLendingIsOverdue() {
        assertThat(policy.evaluateNewLending(history(1, 1))).isEqualTo(PolicyDecision.reject(RejectionReason.OVERDUE_LENDING));
    }

    @Test
    void renewalUsesThePeriodOfTheCurrentActiveCount() {
        assertThat(policy.evaluateRenewal(new LendingToRenew(false, 0), history(1, 0))).isEqualTo(PolicyDecision.allow(21));
        assertThat(policy.evaluateRenewal(new LendingToRenew(false, 0), history(4, 0))).isEqualTo(PolicyDecision.allow(7));
    }

    @Test
    void renewalHasNoRenewalCountLimit() {
        assertThat(policy.evaluateRenewal(new LendingToRenew(false, 50), history(1, 0)).allowed()).isTrue();
    }

    @Test
    void rejectsRenewalOfAnOverdueLending() {
        assertThat(policy.evaluateRenewal(new LendingToRenew(true, 0), history(1, 1)))
                .isEqualTo(PolicyDecision.reject(RejectionReason.LENDING_OVERDUE));
    }
}
