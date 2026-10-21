package pt.psoft.g1.psoftg1.lendingmanagement.services.policy;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StandardLendingPolicyTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);
    // max 3 active, 15-day lendings, 2 renewals of 7 days
    private final StandardLendingPolicy policy = new StandardLendingPolicy(new StandardPolicyParameters(3, 15, 2, 7));

    private static ReaderLendingHistory history(int active, int overdue) {
        return new ReaderLendingHistory(TODAY, active, overdue, 0, 0);
    }

    @Test
    void allowsNewLendingBelowTheLimitWithTheLendingPeriod() {
        assertThat(policy.evaluateNewLending(history(2, 0))).isEqualTo(PolicyDecision.allow(15));
    }

    @Test
    void rejectsNewLendingWhenTheLimitIsReached() {
        assertThat(policy.evaluateNewLending(history(3, 0))).isEqualTo(PolicyDecision.reject(RejectionReason.MAX_ACTIVE_LENDINGS));
    }

    @Test
    void allowsTheFirstLendingOfAReaderWithNoHistory() {
        assertThat(policy.evaluateNewLending(history(0, 0)).allowed()).isTrue();
    }

    @Test
    void rejectsNewLendingWhenAnyLendingIsOverdue() {
        assertThat(policy.evaluateNewLending(history(1, 1))).isEqualTo(PolicyDecision.reject(RejectionReason.OVERDUE_LENDING));
    }

    @Test
    void overdueIsReportedBeforeTheLimit() {
        assertThat(policy.evaluateNewLending(history(3, 1)).reason()).isEqualTo(RejectionReason.OVERDUE_LENDING);
    }

    @Test
    void allowsRenewalBelowTheRenewalLimitWithTheRenewalPeriod() {
        assertThat(policy.evaluateRenewal(new LendingToRenew(false, 1), history(1, 0))).isEqualTo(PolicyDecision.allow(7));
    }

    @Test
    void rejectsRenewalWhenTheRenewalLimitIsReached() {
        assertThat(policy.evaluateRenewal(new LendingToRenew(false, 2), history(1, 0)))
                .isEqualTo(PolicyDecision.reject(RejectionReason.MAX_RENEWALS));
    }

    @Test
    void rejectsRenewalOfAnOverdueLending() {
        assertThat(policy.evaluateRenewal(new LendingToRenew(true, 0), history(1, 1)))
                .isEqualTo(PolicyDecision.reject(RejectionReason.LENDING_OVERDUE));
    }

    @Test
    void zeroMaxRenewalsForbidsEveryRenewal() {
        StandardLendingPolicy noRenewals = new StandardLendingPolicy(new StandardPolicyParameters(3, 15, 0, 7));
        assertThat(noRenewals.evaluateRenewal(new LendingToRenew(false, 0), history(1, 0)).reason())
                .isEqualTo(RejectionReason.MAX_RENEWALS);
    }

    @Test
    void policyDoesNotChangeWhenParametersChange() {
        StandardLendingPolicy longer = new StandardLendingPolicy(new StandardPolicyParameters(3, 21, 2, 7));
        assertThat(longer.evaluateNewLending(history(0, 0)).periodDays()).isEqualTo(21);
    }

    @Test
    void historyCannotHaveMoreOverdueThanActiveLendings() {
        assertThatThrownBy(() -> history(1, 2)).isInstanceOf(IllegalArgumentException.class);
    }
}
