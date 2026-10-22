package pt.psoft.g1.psoftg1.lendingmanagement.services.policy;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AdaptiveEligibilityRuleTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);
    private final LendingPolicy adaptive = new AdaptiveLendingPolicy(new AdaptivePolicyParameters(5, 2, 4, 21, 15, 7));
    // needs at least 5 completed lendings, at least 90% on time, no overdue lending
    private final AdaptiveEligibilityRule rule = new AdaptiveEligibilityRule(new PolicySelectionParameters(6, 5, 90.0), adaptive);

    private static ReaderLendingHistory history(int overdue, int completed, int onTime) {
        return new ReaderLendingHistory(TODAY, overdue, overdue, completed, onTime);
    }

    @Test
    void qualifiesWhenAllConditionsHold() {
        assertThat(rule.select(history(0, 10, 10))).contains(adaptive);
    }

    @Test
    void qualifiesWithExactlyTheMinimumCompletedLendings() {
        assertThat(rule.select(history(0, 5, 5))).contains(adaptive);
    }

    @Test
    void doesNotQualifyOneCompletedLendingBelowTheMinimum() {
        assertThat(rule.select(history(0, 4, 4))).isEmpty();
    }

    @Test
    void qualifiesWithExactlyTheMinimumOnTimeRatio() {
        assertThat(rule.select(history(0, 10, 9))).contains(adaptive);
    }

    @Test
    void doesNotQualifyJustBelowTheMinimumOnTimeRatio() {
        assertThat(rule.select(history(0, 20, 17))).isEmpty();   // 85%
    }

    @Test
    void doesNotQualifyWithACurrentlyOverdueLending() {
        assertThat(rule.select(history(1, 10, 10))).isEmpty();
    }

    @Test
    void readerWithoutHistoryDoesNotQualify() {
        assertThat(rule.select(history(0, 0, 0))).isEmpty();
    }

    @Test
    void ratioOfZeroPercentAcceptsAnyOnTimeCount() {
        AdaptiveEligibilityRule lenient = new AdaptiveEligibilityRule(new PolicySelectionParameters(6, 1, 0.0), adaptive);
        assertThat(lenient.select(history(0, 3, 0))).contains(adaptive);
    }
}
