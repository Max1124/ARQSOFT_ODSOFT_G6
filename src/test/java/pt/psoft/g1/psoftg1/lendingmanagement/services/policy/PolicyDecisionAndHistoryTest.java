package pt.psoft.g1.psoftg1.lendingmanagement.services.policy;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolicyDecisionAndHistoryTest {

    @Test
    void allowedDecisionCarriesThePeriodAndNoReason() {
        PolicyDecision decision = PolicyDecision.allow(15);
        assertThat(decision.allowed()).isTrue();
        assertThat(decision.periodDays()).isEqualTo(15);
        assertThat(decision.reason()).isNull();
    }

    @Test
    void rejectedDecisionCarriesTheReasonAndNoPeriod() {
        PolicyDecision decision = PolicyDecision.reject(RejectionReason.MAX_RENEWALS);
        assertThat(decision.allowed()).isFalse();
        assertThat(decision.periodDays()).isZero();
        assertThat(decision.reason().message()).isNotBlank();
    }

    @Test
    void allowedDecisionNeedsAPositivePeriod() {
        assertThatThrownBy(() -> PolicyDecision.allow(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectedDecisionNeedsAReason() {
        assertThatThrownBy(() -> new PolicyDecision(false, 0, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void historyRejectsNegativeCounts() {
        assertThatThrownBy(() -> new ReaderLendingHistory(LocalDate.of(2026, 3, 10), -1, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void historyCannotHaveMoreOnTimeThanCompletedLendings() {
        assertThatThrownBy(() -> new ReaderLendingHistory(LocalDate.of(2026, 3, 10), 0, 0, 2, 3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void historyNeedsAReferenceDate() {
        assertThatThrownBy(() -> new ReaderLendingHistory(null, 0, 0, 0, 0)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void lendingToRenewRejectsNegativeRenewals() {
        assertThatThrownBy(() -> new LendingToRenew(false, -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
