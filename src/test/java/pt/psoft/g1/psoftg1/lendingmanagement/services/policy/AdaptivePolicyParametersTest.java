package pt.psoft.g1.psoftg1.lendingmanagement.services.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdaptivePolicyParametersTest {

    @Test
    void validValuesAreKept() {
        AdaptivePolicyParameters p = new AdaptivePolicyParameters(5, 2, 4, 21, 15, 7);

        assertThat(p.maxActiveLendings()).isEqualTo(5);
        assertThat(p.firstThreshold()).isEqualTo(2);
        assertThat(p.secondThreshold()).isEqualTo(4);
        assertThat(p.longPeriodDays()).isEqualTo(21);
        assertThat(p.standardPeriodDays()).isEqualTo(15);
        assertThat(p.shortPeriodDays()).isEqualTo(7);
    }

    @Test
    void smallestValidThresholdsAreAccepted() {
        assertThat(new AdaptivePolicyParameters(3, 1, 2, 3, 2, 1)).isNotNull();
    }

    @Test
    void firstThresholdMustBePositive() {
        assertThatThrownBy(() -> new AdaptivePolicyParameters(5, 0, 4, 21, 15, 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FirstThreshold=0")
                .hasMessageContaining("3.5");
    }

    @Test
    void firstThresholdMustBeBelowSecond() {
        assertThatThrownBy(() -> new AdaptivePolicyParameters(5, 4, 4, 21, 15, 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FirstThreshold=4")
                .hasMessageContaining("SecondThreshold (4)");
    }

    @Test
    void secondThresholdMustBeBelowMaxActiveLendings() {
        assertThatThrownBy(() -> new AdaptivePolicyParameters(4, 2, 4, 21, 15, 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SecondThreshold=4")
                .hasMessageContaining("MaxActiveLendings (4)");
    }

    @Test
    void longPeriodMustBeAboveStandardPeriod() {
        assertThatThrownBy(() -> new AdaptivePolicyParameters(5, 2, 4, 15, 15, 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LongPeriod=15")
                .hasMessageContaining("StandardPeriod (15)");
    }

    @Test
    void standardPeriodMustBeAboveShortPeriod() {
        assertThatThrownBy(() -> new AdaptivePolicyParameters(5, 2, 4, 21, 7, 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("StandardPeriod=7")
                .hasMessageContaining("ShortPeriod (7)");
    }

    @Test
    void shortPeriodMustBePositive() {
        assertThatThrownBy(() -> new AdaptivePolicyParameters(5, 2, 4, 21, 15, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ShortPeriod=0")
                .hasMessageContaining("3.4");
    }

    @Test
    void maxActiveLendingsMustBePositive() {
        assertThatThrownBy(() -> new AdaptivePolicyParameters(0, 2, 4, 21, 15, 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MaxActiveLendings=0")
                .hasMessageContaining("3.1");
    }
}
