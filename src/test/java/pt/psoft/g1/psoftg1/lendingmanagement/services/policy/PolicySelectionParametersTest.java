package pt.psoft.g1.psoftg1.lendingmanagement.services.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolicySelectionParametersTest {

    @Test
    void validValuesAreKept() {
        PolicySelectionParameters p = new PolicySelectionParameters(6, 5, 90.0);

        assertThat(p.historyPeriodMonths()).isEqualTo(6);
        assertThat(p.minCompletedLendings()).isEqualTo(5);
        assertThat(p.minOnTimeRatioPercent()).isEqualTo(90.0);
    }

    @Test
    void ratioBoundariesAreAccepted() {
        assertThat(new PolicySelectionParameters(1, 1, 0.0).minOnTimeRatioPercent()).isZero();
        assertThat(new PolicySelectionParameters(1, 1, 100.0).minOnTimeRatioPercent()).isEqualTo(100.0);
    }

    @Test
    void ratioBelowZeroIsRejected() {
        assertThatThrownBy(() -> new PolicySelectionParameters(6, 5, -0.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MinOnTimeRatio=-0.1")
                .hasMessageContaining("between 0 and 100");
    }

    @Test
    void ratioAbove100IsRejected() {
        assertThatThrownBy(() -> new PolicySelectionParameters(6, 5, 100.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MinOnTimeRatio=100.1");
    }

    @Test
    void nonNumericRatioIsRejected() {
        assertThatThrownBy(() -> new PolicySelectionParameters(6, 5, Double.NaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MinOnTimeRatio");
    }

    @Test
    void historyPeriodMustBePositive() {
        assertThatThrownBy(() -> new PolicySelectionParameters(0, 5, 90.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HistoryPeriod=0")
                .hasMessageContaining("4.2");
    }

    @Test
    void minCompletedLendingsMustBePositive() {
        assertThatThrownBy(() -> new PolicySelectionParameters(6, 0, 90.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MinCompletedLendings=0")
                .hasMessageContaining("4.2.1");
    }
}
