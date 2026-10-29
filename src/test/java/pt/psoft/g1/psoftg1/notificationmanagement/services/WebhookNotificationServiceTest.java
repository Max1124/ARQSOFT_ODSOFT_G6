package pt.psoft.g1.psoftg1.notificationmanagement.services;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Class SUT: WebhookNotificationService. ReaderRepository is a double and delivery goes to a mock server bound to the
 * framework client the channel uses, so no message leaves the test (TST-NOT-02, CON-TST-8).
 */
class WebhookNotificationServiceTest {
    private static final String TARGET = "http://webhook-receiver.test/library";
    private static final String READER_NUMBER = "2026/1";
    private static final LocalDate LIMIT = LocalDate.of(2026, 3, 13);

    private final ReaderRepository readers = mock(ReaderRepository.class);
    private final RestClient.Builder http = RestClient.builder();
    private final MockRestServiceServer receiver = MockRestServiceServer.bindTo(http).build();
    private final WebhookNotificationService channel = new WebhookNotificationService(readers, http);

    private ReaderDetails readerExists() {
        final ReaderDetails reader = new ReaderDetails(1, Reader.newReader("manuel@example.com", "Manuelino123!", "Manuel Sarapinto das Coives"),
                "2000-01-01", "919191919", true, true, true, null, null);
        when(readers.findByReaderNumber(READER_NUMBER)).thenReturn(Optional.of(reader));
        return reader;
    }

    @Test
    void overdueEventIsPostedToTheReadersUrlWithReaderLendingDaysAndFine() {
        readerExists().defineNotificationWebhookUrl(TARGET);
        receiver.expect(requestTo(TARGET))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.event").value("OVERDUE"))
                .andExpect(jsonPath("$.readerNumber").value(READER_NUMBER))
                .andExpect(jsonPath("$.lendingNumber").value("2026/5"))
                .andExpect(jsonPath("$.bookTitle").value("O Inspetor Max"))
                .andExpect(jsonPath("$.limitDate").value("2026-03-13"))
                .andExpect(jsonPath("$.daysOverdue").value(5))
                .andExpect(jsonPath("$.fineValue").value(10.0))
                .andRespond(withSuccess());

        channel.send(READER_NUMBER, LendingNotification.overdue("2026/5", "O Inspetor Max", LIMIT, 5, 1000));

        receiver.verify();
    }

    @Test
    void dueDateEventIsPostedWithoutAFine() {
        readerExists().defineNotificationWebhookUrl(TARGET);
        receiver.expect(requestTo(TARGET))
                .andExpect(jsonPath("$.event").value("DUE_DATE_APPROACHING"))
                .andExpect(jsonPath("$.readerNumber").value(READER_NUMBER))
                .andExpect(jsonPath("$.limitDate").value("2026-03-13"))
                .andExpect(jsonPath("$.fineValue").value(0.0))
                .andRespond(withSuccess());

        channel.send(READER_NUMBER, LendingNotification.dueDateApproaching("2026/5", "O Inspetor Max", LIMIT));

        receiver.verify();
    }

    @Test
    void readerWithoutAWebhookUrlIsReportedAndNothingIsSent() {
        readerExists();

        assertThatThrownBy(() -> channel.send(READER_NUMBER, LendingNotification.dueDateApproaching("2026/5", "O Inspetor Max", LIMIT)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Reader 2026/1 has no webhook URL");
        receiver.verify();
    }

    @Test
    void unknownReaderIsReportedAndNothingIsSent() {
        when(readers.findByReaderNumber(READER_NUMBER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> channel.send(READER_NUMBER, LendingNotification.dueDateApproaching("2026/5", "O Inspetor Max", LIMIT)))
                .isInstanceOf(NotFoundException.class);
        receiver.verify();
    }

    @Test
    void receiverFailureIsReportedToTheCaller() {
        readerExists().defineNotificationWebhookUrl(TARGET);
        receiver.expect(requestTo(TARGET)).andRespond(withServerError());

        assertThatThrownBy(() -> channel.send(READER_NUMBER, LendingNotification.dueDateApproaching("2026/5", "O Inspetor Max", LIMIT)))
                .isInstanceOf(RestClientException.class);
    }

    @Test
    void identifiesItselfAsTheWebhookChannel() {
        assertThat(channel.channel()).isEqualTo("webhook");
    }
}
