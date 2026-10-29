package pt.psoft.g1.psoftg1.notificationmanagement.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Class SUT: SMSNotificationService. ReaderRepository is a double and delivery goes to a mock server bound to the
 * framework client the channel uses, so no message leaves the test (TST-NOT-02, CON-TST-8).
 */
class SMSNotificationServiceTest {
    private static final String GATEWAY = "http://sms-gateway.test:8080/message";
    private static final String READER_NUMBER = "2026/1";
    private static final LocalDate LIMIT = LocalDate.of(2026, 3, 13);
    private static final String BASIC_AUTH = "Basic "
            + Base64.getEncoder().encodeToString("gateway-user:gateway-pass".getBytes(StandardCharsets.UTF_8));

    private final ReaderRepository readers = mock(ReaderRepository.class);
    private final RestClient.Builder http = RestClient.builder();
    private final MockRestServiceServer gateway = MockRestServiceServer.bindTo(http).build();
    private final SMSNotificationService channel =
            new SMSNotificationService(readers, http, GATEWAY, "gateway-user", "gateway-pass", "+351");

    private void readerExists() {
        final ReaderDetails reader = new ReaderDetails(1, Reader.newReader("manuel@example.com", "Manuelino123!", "Manuel Sarapinto das Coives"),
                "2000-01-01", "919191919", true, true, true, null, null);
        when(readers.findByReaderNumber(READER_NUMBER)).thenReturn(Optional.of(reader));
    }

    @Test
    void dueDateNoticeIsSentToTheReadersPhoneNumberWithTheCountryCode() {
        readerExists();
        gateway.expect(requestTo(GATEWAY))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, BASIC_AUTH))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.phoneNumbers.length()").value(1))
                .andExpect(jsonPath("$.phoneNumbers[0]").value("+351919191919"))
                .andExpect(jsonPath("$.textMessage.text").value("Library: lending 2026/5 ('O Inspetor Max') is due on 2026-03-13."))
                .andRespond(withSuccess());

        channel.send(READER_NUMBER, LendingNotification.dueDateApproaching("2026/5", "O Inspetor Max", LIMIT));

        gateway.verify();
    }

    @Test
    void overdueNoticeCarriesTheDaysOverdueAndTheFine() {
        readerExists();
        gateway.expect(requestTo(GATEWAY))
                .andExpect(jsonPath("$.phoneNumbers[0]").value("+351919191919"))
                .andExpect(jsonPath("$.textMessage.text", allOf(containsString("2026/5"), containsString("was due on 2026-03-13"),
                        containsString("5 day(s) overdue"), containsString("A fine of 10.00€ is being applied"))))
                .andRespond(withSuccess());

        channel.send(READER_NUMBER, LendingNotification.overdue("2026/5", "O Inspetor Max", LIMIT, 5, 1000));

        gateway.verify();
    }

    @Test
    void unknownReaderIsReportedAndNothingIsSent() {
        when(readers.findByReaderNumber(READER_NUMBER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> channel.send(READER_NUMBER, LendingNotification.dueDateApproaching("2026/5", "O Inspetor Max", LIMIT)))
                .isInstanceOf(NotFoundException.class);
        gateway.verify();
    }

    @Test
    void gatewayFailureIsReportedToTheCaller() {
        readerExists();
        gateway.expect(requestTo(GATEWAY)).andRespond(withServerError());

        assertThatThrownBy(() -> channel.send(READER_NUMBER, LendingNotification.dueDateApproaching("2026/5", "O Inspetor Max", LIMIT)))
                .isInstanceOf(RestClientException.class);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "351", "+", "+3512", "+35a"})
    void malformedCountryCodeIsRejected(String countryCode) {
        assertThatThrownBy(() -> new SMSNotificationService(readers, RestClient.builder(), GATEWAY, "u", "p", countryCode))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("country code");
    }

    @Test
    void identifiesItselfAsTheSmsChannel() {
        assertThat(channel.channel()).isEqualTo("sms");
    }
}
