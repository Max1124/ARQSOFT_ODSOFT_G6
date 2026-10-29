package pt.psoft.g1.psoftg1.readermanagement.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.notificationmanagement.services.NotificationService;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Class SUT: NotificationPreferenceService (TST-NOT-04). The repository, the enabled channels and the reader entity
 * are doubles, so only the validation rule and the storage call are under test (CFG-NOT-02). The webhook URL cases
 * use a real ReaderDetails where the entity's own validation is part of the expected outcome.
 */
class NotificationPreferenceServiceTest {
    private static final String USERNAME = "ana@example.com";
    private static final String WEBHOOK_URL = "https://hooks.example.com/library";
    private final ReaderRepository readers = mock(ReaderRepository.class);

    private static List<NotificationService> enabled(String... ids) {
        return Arrays.stream(ids).map(id -> {
            final NotificationService channel = mock(NotificationService.class);
            when(channel.channel()).thenReturn(id);
            return channel;
        }).toList();
    }

    private ReaderDetails realReaderExists() {
        final ReaderDetails reader = new ReaderDetails(1, Reader.newReader(USERNAME, "Manuelino123!", "Ana Maria"),
                "2000-01-01", "919191919", true, true, true, null, null);
        when(readers.findByUsername(USERNAME)).thenReturn(Optional.of(reader));
        when(readers.save(reader)).thenReturn(reader);
        return reader;
    }

    @Test
    void choiceWithDisabledChannelsIsRejectedNamingThemAndNothingIsStored() {
        final NotificationPreferenceService service = new NotificationPreferenceService(readers, enabled("email"));

        assertThatThrownBy(() -> service.validateAndStore(USERNAME, List.of("email", "sms", "webhook"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Notification channel not enabled: sms, webhook");
        verifyNoInteractions(readers);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void emptyChoiceIsRejectedAndNothingIsStored(List<String> channels) {
        final NotificationPreferenceService service = new NotificationPreferenceService(readers, enabled("email", "sms"));

        assertThatThrownBy(() -> service.validateAndStore(USERNAME, channels, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one notification channel must be chosen");
        verifyNoInteractions(readers);
    }

    @Test
    void validChoiceIsStoredForThatReaderWithoutRepetitions() {
        final NotificationPreferenceService service = new NotificationPreferenceService(readers, enabled("email", "sms"));
        final ReaderDetails reader = mock(ReaderDetails.class);
        when(readers.findByUsername(USERNAME)).thenReturn(Optional.of(reader));
        when(readers.save(reader)).thenReturn(reader);

        final ReaderDetails stored = service.validateAndStore(USERNAME, List.of("sms", "email", "sms"), null);

        assertThat(stored).isSameAs(reader);
        verify(reader).chooseNotificationChannels(Set.of("sms", "email"));
        verify(reader, never()).defineNotificationWebhookUrl(any());
        verify(readers).save(reader);
    }

    @Test
    void unknownReaderIsNotFound() {
        final NotificationPreferenceService service = new NotificationPreferenceService(readers, enabled("email"));
        when(readers.findByUsername(USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validateAndStore(USERNAME, List.of("email"), null))
                .isInstanceOf(NotFoundException.class);
    }

    // webhook URL (the webhook channel posts to the reader's own URL)

    @Test
    void webhookChoiceWithAUrlStoresBoth() {
        final NotificationPreferenceService service = new NotificationPreferenceService(readers, enabled("email", "webhook"));
        final ReaderDetails reader = realReaderExists();

        service.validateAndStore(USERNAME, List.of("webhook"), WEBHOOK_URL);

        assertThat(reader.getNotificationChannels()).containsExactly("webhook");
        assertThat(reader.getNotificationWebhookUrl()).contains(WEBHOOK_URL);
        verify(readers).save(reader);
    }

    @Test
    void webhookChoiceWithoutAnyUrlIsRejectedAndNothingIsStored() {
        final NotificationPreferenceService service = new NotificationPreferenceService(readers, enabled("email", "webhook"));
        final ReaderDetails reader = realReaderExists();

        assertThatThrownBy(() -> service.validateAndStore(USERNAME, List.of("email", "webhook"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The webhook channel requires a webhook URL");
        assertThat(reader.getNotificationChannels()).isEmpty();
        verify(readers, never()).save(any());
    }

    @Test
    void storedUrlIsKeptWhenTheRequestGivesNone() {
        final NotificationPreferenceService service = new NotificationPreferenceService(readers, enabled("email", "webhook"));
        final ReaderDetails reader = realReaderExists();
        service.validateAndStore(USERNAME, List.of("webhook"), WEBHOOK_URL);

        service.validateAndStore(USERNAME, List.of("email", "webhook"), null);

        assertThat(reader.getNotificationChannels()).containsExactlyInAnyOrder("email", "webhook");
        assertThat(reader.getNotificationWebhookUrl()).contains(WEBHOOK_URL);
    }

    @Test
    void invalidUrlIsRejectedAndNothingIsStored() {
        final NotificationPreferenceService service = new NotificationPreferenceService(readers, enabled("webhook"));
        final ReaderDetails reader = realReaderExists();

        assertThatThrownBy(() -> service.validateAndStore(USERNAME, List.of("webhook"), "ftp://hooks.example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Webhook URL is not valid: ftp://hooks.example.com");
        assertThat(reader.getNotificationWebhookUrl()).isEmpty();
        verify(readers, never()).save(any());
    }
}
