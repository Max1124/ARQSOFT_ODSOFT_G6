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
 * Integration of the webhook channel over HTTP, with Mockoon as the receiver: port 8027, a POST /webhook route
 * answering 200. The reader first registers that URL through the API, and the channel then posts the event to it.
 * One test also reads the request back from Mockoon. Not part of ./mvnw test; the pipelines run it with
 * jenkins/run-notification-its.sh.
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

    @Test
    void dueDateEventReachesTheMockoonWebhookUrl(CapturedOutput output) throws Exception {
        choose("[\"webhook\"]", MOCKOON_WEBHOOK).andExpect(status().isOk());
        clearMockoonRequests();

        lendingService.checkDueAndOverdueLendings();

        assertChannelDidNotFail(output, "webhook");
        assertMockoonAnsweredOneRequest("/webhook", "DUE_DATE_APPROACHING", lendingNumber);
    }
}
