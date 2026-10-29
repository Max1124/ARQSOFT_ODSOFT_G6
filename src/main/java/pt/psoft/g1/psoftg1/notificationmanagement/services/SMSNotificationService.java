package pt.psoft.g1.psoftg1.notificationmanagement.services;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;

/**
 * SMS channel (EXT-NOT-02). The recipient is the phone number in {@code ReaderDetails} (CON-NOT-4), prefixed with
 * the country code set at setup. Delivery is through SMS Gateway for Android (local server mode): a JSON POST to
 * the phone's /message endpoint with basic authentication; the phone sends the SMS from its SIM.
 * The HTTP client stays inside this class (CON-NOT-5).
 */
public class SMSNotificationService implements NotificationService {
    public static final String CHANNEL = "sms";

    private final ReaderRepository readerRepository;
    private final RestClient http;
    private final String gatewayUrl;
    private final String countryCode;

    // The framework builder is received so tests can bind it to a mock server; 0 real messages (TST-NOT-02).
    // gatewayUrl is the phone's message endpoint (e.g. http://192.168.1.50:8080/message); username and password
    // are the credentials shown by the app.
    public SMSNotificationService(ReaderRepository readerRepository, RestClient.Builder http, String gatewayUrl,
                                  String username, String password, String countryCode) {
        if (countryCode == null || !countryCode.matches("\\+\\d{1,3}")) {
            throw new IllegalArgumentException("SMS country code must be '+' followed by 1 to 3 digits: " + countryCode);
        }
        this.readerRepository = readerRepository;
        this.http = http.defaultHeaders(headers -> headers.setBasicAuth(username, password)).build();
        this.gatewayUrl = gatewayUrl;
        this.countryCode = countryCode;
    }

    @Override
    public String channel() {
        return CHANNEL;
    }

    @Override
    public void send(String readerNumber, LendingNotification notification) {
        final ReaderDetails reader = readerRepository.findByReaderNumber(readerNumber)
                .orElseThrow(() -> new NotFoundException(ReaderDetails.class, readerNumber));

        http.post().uri(gatewayUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SmsMessage(new TextMessage(text(notification)),
                        List.of(countryCode + reader.getPhoneNumber())))
                .retrieve()
                .toBodilessEntity();
    }

    // Short text: one SMS per event.
    private static String text(LendingNotification n) {
        if (n.type() == LendingNotification.Type.OVERDUE) {
            return "The book '" + n.bookTitle() + "' (lending " + n.lendingNumber() + ") was due on " + n.limitDate()
                    + " and is " + n.daysOverdue() + " day(s) overdue. A fine of " + formatEuros(n.fineValueInCents())
                    + " is being applied.";
        }
        return "Library: lending " + n.lendingNumber() + " ('" + n.bookTitle() + "') is due on " + n.limitDate() + ".";
    }

    private static String formatEuros(long cents) {
        return java.math.BigDecimal.valueOf(cents, 2).toPlainString() + "€";
    }

    // Request body of the gateway's /message endpoint.
    public record SmsMessage(TextMessage textMessage, List<String> phoneNumbers) {}

    public record TextMessage(String text) {}
}
