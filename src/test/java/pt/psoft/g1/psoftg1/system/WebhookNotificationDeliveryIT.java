package pt.psoft.g1.psoftg1.system;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.test.context.TestPropertySource;
import pt.psoft.g1.psoftg1.testutils.NotificationDeliveryITSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration with a real webhook receiver: Mockoon on this machine, environment on port 8027 with a
 * POST /webhook route answering 200. The reader first registers that URL as their webhook through the API;
 * the webhook channel then posts the event to it. Mockoon answering 2xx is checked here (any other answer is
 * logged as a failed channel); the payload is visible in Mockoon's Logs tab.
 * Not part of ./mvnw test; run with ./mvnw test -Dtest=WebhookNotificationDeliveryIT while Mockoon is running.
 */
@TestPropertySource(properties = {"notification.channels=webhook", "notification.due-date-notice-days=15"})
class WebhookNotificationDeliveryIT extends NotificationDeliveryITSupport {
    private static final String MOCKOON_WEBHOOK = "http://localhost:8027/webhook";

    @BeforeAll
    static void mockoonIsRunning() {
        requireListening("Mockoon", "localhost", 8027, "Start the Mockoon environment with the POST /webhook route.");
    }

    @Test
    void readerRegistersTheMockoonUrlAndTheDueDateEventIsPostedToIt(CapturedOutput output) throws Exception {
        assertReaderIsAsSpecified();

        choose("[\"webhook\"]", MOCKOON_WEBHOOK)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channels[0]").value("webhook"));
        assertThat(readerRepository.findByReaderNumber(reader.getReaderNumber()).orElseThrow().getNotificationWebhookUrl())
                .contains(MOCKOON_WEBHOOK);

        lendingService.checkDueAndOverdueLendings();

        assertChannelDidNotFail(output, "webhook");
    }
}
