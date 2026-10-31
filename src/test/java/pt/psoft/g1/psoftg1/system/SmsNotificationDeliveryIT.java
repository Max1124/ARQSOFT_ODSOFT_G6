package pt.psoft.g1.psoftg1.system;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.test.context.TestPropertySource;
import pt.psoft.g1.psoftg1.testutils.NotificationDeliveryITSupport;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration of the SMS channel over HTTP, with Mockoon standing in for SMS Gateway for Android: the gateway address
 * is notification.sms.gateway-url in the test application.properties. No SMS is sent. The request reaching the
 * POST /message route, with the reader's number and the lending, is checked here.
 * Not part of ./mvnw test; the pipelines run it with jenkins/run-notification-its.sh.
 * For a demo with the real gateway, set its address and credentials in the test application.properties and comment
 * out the two Mockoon calls in the test; the SMS arriving is then checked on the phone.
 * */
@TestPropertySource(properties = {"notification.channels=sms", "notification.due-date-notice-days=15",
        "notification.sms.country-code=+351"})
class SmsNotificationDeliveryIT extends NotificationDeliveryITSupport {

    @BeforeAll
    static void mockoonIsRunning() {
        requireListening("Mockoon", "localhost", 8027, "Start the Mockoon environment with the POST /message route.");
    }

    @Test
    void dueDateNoticeIsAcceptedByThePhoneGatewayForTheReadersNumber(CapturedOutput output) throws Exception {
        assertReaderIsAsSpecified();
        choose("[\"sms\"]").andExpect(status().isOk());

        lendingService.checkDueAndOverdueLendings();

        // A rejected request (wrong credentials, wrong address, gateway offline) is logged as a failed channel.
        assertChannelDidNotFail(output, "sms");
    }

    @Test
    void dueDateNoticeReachesTheMockoonGatewayUrl(CapturedOutput output) throws Exception {
        choose("[\"sms\"]").andExpect(status().isOk());
        clearMockoonRequests();

        lendingService.checkDueAndOverdueLendings();

        assertChannelDidNotFail(output, "sms");
        assertMockoonAnsweredOneRequest("/message", "+351" + READER_PHONE, lendingNumber);
    }
}
