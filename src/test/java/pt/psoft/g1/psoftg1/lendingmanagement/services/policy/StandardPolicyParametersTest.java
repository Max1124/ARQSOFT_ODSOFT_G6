package pt.psoft.g1.psoftg1.lendingmanagement.services.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StandardPolicyParametersTest {

    @Test
    void validValuesAreKept() {
        StandardPolicyParameters p = new StandardPolicyParameters(3, 15, 2, 7);

        assertThat(p.maxActiveLendings()).isEqualTo(3);
        assertThat(p.lendingPeriodDays()).isEqualTo(15);
        assertThat(p.maxRenewals()).isEqualTo(2);
        assertThat(p.renewalPeriodDays()).isEqualTo(7);
    }

    @Test
    void zeroMaxRenewalsIsValid() {
        assertThat(new StandardPolicyParameters(3, 15, 0, 7).maxRenewals()).isZero();
    }

    @Test
    void maxActiveLendingsMustBePositive() {
        assertThatThrownBy(() -> new StandardPolicyParameters(0, 15, 2, 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MaxActiveLendings=0")
                .hasMessageContaining("must be > 0")
                .hasMessageContaining("2.1");
    }

    @Test
    void lendingPeriodMustBePositive() {
        assertThatThrownBy(() -> new StandardPolicyParameters(3, 0, 2, 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LendingPeriod=0")
                .hasMessageContaining("2.2");
    }

    @Test
    void maxRenewalsCannotBeNegative() {
        assertThatThrownBy(() -> new StandardPolicyParameters(3, 15, -1, 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MaxRenewals=-1")
                .hasMessageContaining("must be >= 0");
    }

    @Test
    void renewalPeriodMustBePositive() {
        assertThatThrownBy(() -> new StandardPolicyParameters(3, 15, 2, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RenewalPeriod=0")
                .hasMessageContaining("2.3");
    }
}
