package pt.psoft.g1.psoftg1.notificationmanagement.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Class SUT: the notification parameter object; one case on each side of the boundary. */
class NotificationParametersTest {

    @Test
    void smallestPositiveValueIsAccepted() {
        assertThat(new NotificationParameters(1).dueDateNoticeDays()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void nonPositiveValueIsRejectedNamingTheParameter(int days) {
        assertThatThrownBy(() -> new NotificationParameters(days))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("DueDateNoticeDays=" + days + " must be > 0");
    }
}
