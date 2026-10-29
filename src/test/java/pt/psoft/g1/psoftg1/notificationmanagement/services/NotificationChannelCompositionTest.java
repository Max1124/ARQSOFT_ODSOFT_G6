package pt.psoft.g1.psoftg1.notificationmanagement.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Class SUT: the channel composition (CFG-NOT-01). Installed channels are plain fixtures; no Spring. */
class NotificationChannelCompositionTest {
    private final List<String> instantiated = new ArrayList<>();
    private NotificationChannelComposition composition;

    record FixedChannel(String channel) implements NotificationService {
        @Override
        public void send(String readerNumber, LendingNotification notification) {
            // not used: these tests only check which channels are created
        }
    }

    @BeforeEach
    void installEmailSmsAndWebhook() {
        final Map<String, Supplier<NotificationService>> installed = new LinkedHashMap<>();
        for (String id : List.of("email", "sms", "webhook")) {
            installed.put(id, () -> {
                instantiated.add(id);
                return new FixedChannel(id);
            });
        }
        composition = new NotificationChannelComposition(installed);
    }

    private static List<String> idsOf(List<NotificationService> channels) {
        return channels.stream().map(NotificationService::channel).toList();
    }

    @Test
    void onlyTheListedChannelsAreInstantiatedInTheConfiguredOrder() {
        final List<NotificationService> enabled = composition.enabledChannels("webhook, email");

        assertThat(idsOf(enabled)).containsExactly("webhook", "email");
        assertThat(instantiated).containsExactly("webhook", "email"); // sms is never created: 0 notifications
    }

    @Test
    void oneChannelIsEnough() {
        assertThat(idsOf(composition.enabledChannels("sms"))).containsExactly("sms");
    }

    @Test
    void unknownIdentifierStopsStartupNamingItBeforeAnyChannelIsCreated() {
        assertThatThrownBy(() -> composition.enabledChannels("email,fax"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unknown notification channel 'fax'; installed: email, sms, webhook");
        assertThat(instantiated).isEmpty();
    }

    @Test
    void repeatedIdentifierStopsStartup() {
        assertThatThrownBy(() -> composition.enabledChannels("email,sms,email"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Notification channel 'email' is listed more than once");
        assertThat(instantiated).isEmpty();
    }

    // Group decision: the administrator must enable at least one channel.
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void missingOrEmptyListStopsStartup(String identifiers) {
        assertThatThrownBy(() -> composition.enabledChannels(identifiers))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("At least one notification channel must be enabled; installed: email, sms, webhook");
    }
}
