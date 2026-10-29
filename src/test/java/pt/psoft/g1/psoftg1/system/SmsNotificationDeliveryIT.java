package pt.psoft.g1.psoftg1.system;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.test.context.TestPropertySource;
import pt.psoft.g1.psoftg1.testutils.NotificationDeliveryITSupport;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration with a real SMS gateway: SMS Gateway for Android in local server mode on the phone. The gateway
 * address and its credentials come from the environment variables NOTIFICATION_SMS_GATEWAY_URL,
 * NOTIFICATION_SMS_USERNAME and NOTIFICATION_SMS_PASSWORD of the shell that runs the test.
 * This sends one real SMS to +351924470976. The gateway accepting the request is checked here; the SMS arriving
 * is checked on the phone. Not part of ./mvnw test; run with ./mvnw test -Dtest=SmsNotificationDeliveryIT.
 */
@TestPropertySource(properties = {"notification.channels=sms", "notification.due-date-notice-days=15",
        "notification.sms.gateway-url=${NOTIFICATION_SMS_GATEWAY_URL}",
        "notification.sms.username=${NOTIFICATION_SMS_USERNAME}",
        "notification.sms.password=${NOTIFICATION_SMS_PASSWORD}",
        "notification.sms.country-code=+351"})
class SmsNotificationDeliveryIT extends NotificationDeliveryITSupport {

    @BeforeAll
    static void gatewayIsConfigured() {
        requireEnvironmentVariable("NOTIFICATION_SMS_GATEWAY_URL");
        requireEnvironmentVariable("NOTIFICATION_SMS_USERNAME");
        requireEnvironmentVariable("NOTIFICATION_SMS_PASSWORD");
    }

    @Test
    void dueDateNoticeIsAcceptedByThePhoneGatewayForTheReadersNumber(CapturedOutput output) throws Exception {
        assertReaderIsAsSpecified();
        choose("[\"sms\"]").andExpect(status().isOk());

        lendingService.checkDueAndOverdueLendings();

        // A rejected request (wrong credentials, wrong address, gateway offline) is logged as a failed channel.
        assertChannelDidNotFail(output, "sms");
    }
}
