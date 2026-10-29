package pt.psoft.g1.psoftg1.system;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;
import pt.psoft.g1.psoftg1.testutils.NotificationDeliveryITSupport;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration with a real SMTP server: Mailpit on this machine (SMTP on localhost:1025, API on localhost:8025).
 * Mailpit keeps the message instead of delivering it, and the test reads it back through Mailpit's API.
 * Not part of ./mvnw test; run with ./mvnw test -Dtest=EmailNotificationDeliveryIT while Mailpit is running.
 */
@TestPropertySource(properties = {"notification.channels=email", "notification.due-date-notice-days=15",
        "notification.email.smtp.host=localhost", "notification.email.smtp.port=1025",
        "notification.email.from=library@localhost"})
class EmailNotificationDeliveryIT extends NotificationDeliveryITSupport {
    private static final String MAILPIT_MESSAGES = "http://localhost:8025/api/v1/messages";

    @BeforeAll
    static void mailpitIsRunning() {
        requireListening("Mailpit SMTP", "localhost", 1025, "Start Mailpit before running this test.");
        requireListening("Mailpit API", "localhost", 8025, "Start Mailpit before running this test.");
    }

    @Test
    void dueDateNoticeReachesMailpitAddressedToTheReadersEmail(CapturedOutput output) throws Exception {
        assertReaderIsAsSpecified();
        final Instant sentAfter = Instant.now().minusSeconds(1);
        choose("[\"email\"]").andExpect(status().isOk());

        lendingService.checkDueAndOverdueLendings();

        assertChannelDidNotFail(output, "email");
        final String expectedSubject = "Lending " + lendingNumber + " is due on " + limitDate;
        assertThat(messagesInMailpitSince(sentAfter))
                .filteredOn(message -> expectedSubject.equals(message.get("Subject")))
                .singleElement()
                .satisfies(message -> {
                    assertThat(JsonPath.<List<String>>read(message, "$.To[*].Address")).containsExactly(READER_EMAIL);
                    assertThat(JsonPath.<String>read(message, "$.From.Address")).isEqualTo("library@localhost");
                    assertThat((String) message.get("Snippet")).contains("O Inspetor Max").contains(lendingNumber)
                            .contains("must be returned by " + limitDate);
                });
    }

    // Mailpit lists the newest messages first; only the ones received during this test are kept.
    private static List<Map<String, Object>> messagesInMailpitSince(Instant since) {
        final String json = RestClient.create().get().uri(MAILPIT_MESSAGES).retrieve().body(String.class);
        final List<Map<String, Object>> messages = JsonPath.read(json, "$.messages");
        return messages.stream()
                .filter(message -> !OffsetDateTime.parse((String) message.get("Created")).toInstant().isBefore(since))
                .toList();
    }
}
