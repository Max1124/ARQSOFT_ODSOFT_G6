package pt.psoft.g1.psoftg1.notificationmanagement.services;

import java.math.BigDecimal;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;

/**
 * Webhook channel (EXT-NOT-02). The target is the webhook URL the reader provided in {@code ReaderDetails}
 * (CON-NOT-4). Delivery is a POST of the event to that URL; the HTTP client stays inside this class (CON-NOT-5).
 */
public class WebhookNotificationService implements NotificationService {
    public static final String CHANNEL = "webhook";

    private final ReaderRepository readerRepository;
    private final RestClient http;

    // The framework builder is received so tests can bind it to a mock server; 0 real messages (TST-NOT-02).
    public WebhookNotificationService(ReaderRepository readerRepository, RestClient.Builder http) {
        this.readerRepository = readerRepository;
        this.http = http.build();
    }

    @Override
    public String channel() {
        return CHANNEL;
    }

    @Override
    public void send(String readerNumber, LendingNotification notification) {
        final ReaderDetails reader = readerRepository.findByReaderNumber(readerNumber)
                .orElseThrow(() -> new NotFoundException(ReaderDetails.class, readerNumber));
        final String targetUrl = reader.getNotificationWebhookUrl()
                .orElseThrow(() -> new IllegalStateException("Reader " + readerNumber + " has no webhook URL"));

        http.post().uri(targetUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new WebhookPayload(notification.type().name(), readerNumber, notification.lendingNumber(),
                        notification.bookTitle(), notification.limitDate().toString(),
                        notification.daysOverdue(), BigDecimal.valueOf(notification.fineValueInCents(), 2)))
                .retrieve()
                .toBodilessEntity();
    }

    // Structured fields instead of a text message, so the receiving system can process the event.
    // fineValue is in euros with two decimals (e.g. 2.50), converted from the cents held by the notification.
    public record WebhookPayload(String event, String readerNumber, String lendingNumber, String bookTitle,
                                 String limitDate, int daysOverdue, BigDecimal fineValue) {}
}
