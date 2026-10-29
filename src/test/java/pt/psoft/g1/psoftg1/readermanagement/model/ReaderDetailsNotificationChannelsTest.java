package pt.psoft.g1.psoftg1.readermanagement.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Transparent-box, domain SUT: the notification channel choice and webhook URL added to ReaderDetails.
 * One case per branch of chooseNotificationChannels (null, empty, valid) plus the copy on read and on write,
 * and one per branch of the WebhookUrl validation.
 */
class ReaderDetailsNotificationChannelsTest {
    private ReaderDetails reader;

    @BeforeEach
    void newReader() {
        reader = new ReaderDetails(1, Reader.newReader("manuel@example.com", "Manuelino123!", "Manuel Sarapinto das Coives"),
                "2000-01-01", "919191919", true, true, true, null, null);
    }

    @Test
    void newReaderHasNotChosenAnyChannel() {
        assertThat(reader.getNotificationChannels()).isEmpty();
    }

    @Test
    void newChoiceReplacesThePreviousOne() {
        reader.chooseNotificationChannels(Set.of("email", "sms"));
        reader.chooseNotificationChannels(Set.of("webhook"));

        assertThat(reader.getNotificationChannels()).containsExactly("webhook");
    }

    @Test
    void emptyChoiceIsRejectedAndThePreviousChoiceIsKept() {
        reader.chooseNotificationChannels(Set.of("email"));

        assertThatThrownBy(() -> reader.chooseNotificationChannels(Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one notification channel must be chosen");
        assertThat(reader.getNotificationChannels()).containsExactly("email");
    }

    @Test
    void missingChoiceIsRejected() {
        assertThatThrownBy(() -> reader.chooseNotificationChannels(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(reader.getNotificationChannels()).isEmpty();
    }

    @Test
    void storedChoiceCannotBeChangedFromOutsideTheEntity() {
        final Set<String> request = new HashSet<>(Set.of("email"));
        reader.chooseNotificationChannels(request);
        request.add("sms");

        assertThat(reader.getNotificationChannels()).containsExactly("email");
        assertThatThrownBy(() -> reader.getNotificationChannels().add("webhook"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // webhook URL: one case per branch of WebhookUrl (empty, unparsable, wrong scheme, no host, valid)

    @Test
    void newReaderHasNoWebhookUrl() {
        assertThat(reader.getNotificationWebhookUrl()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:8027/webhook", "https://hooks.example.com/library?key=1", "HTTPS://hooks.example.com"})
    void httpAndHttpsUrlsAreAccepted(String url) {
        reader.defineNotificationWebhookUrl(url);

        assertThat(reader.getNotificationWebhookUrl()).contains(url);
    }

    @Test
    void surroundingSpacesAreRemoved() {
        reader.defineNotificationWebhookUrl("  https://hooks.example.com/library ");

        assertThat(reader.getNotificationWebhookUrl()).contains("https://hooks.example.com/library");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void emptyUrlIsRejected(String url) {
        assertThatThrownBy(() -> reader.defineNotificationWebhookUrl(url))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Webhook URL must not be empty");
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://exa mple.com", "ftp://hooks.example.com", "hooks.example.com/library", "/webhook", "http:///webhook"})
    void malformedUrlIsRejectedAndThePreviousOneIsKept(String url) {
        reader.defineNotificationWebhookUrl("https://hooks.example.com/library");

        assertThatThrownBy(() -> reader.defineNotificationWebhookUrl(url))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Webhook URL is not valid: " + url);
        assertThat(reader.getNotificationWebhookUrl()).contains("https://hooks.example.com/library");
    }
}
