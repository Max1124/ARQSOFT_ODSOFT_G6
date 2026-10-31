package pt.psoft.g1.psoftg1.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.web.client.RestClient;
import pt.psoft.g1.psoftg1.lendingmanagement.services.LendingService;
import pt.psoft.g1.psoftg1.notificationmanagement.services.EmailNotificationService;
import pt.psoft.g1.psoftg1.notificationmanagement.services.NotificationParameters;
import pt.psoft.g1.psoftg1.notificationmanagement.services.NotificationService;
import pt.psoft.g1.psoftg1.notificationmanagement.services.SMSNotificationService;
import pt.psoft.g1.psoftg1.notificationmanagement.services.WebhookNotificationService;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Setup-time channel composition (TST-NOT-03): starts only the notification configuration with test properties.
 * The repository and the lending service are doubles and no channel sends anything, since only startup is exercised.
 * The SMS gateway credentials, which an installation provides through environment variables, are given as test values.
 */
class NotificationChannelsConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(NotificationChannelsConfig.class)
            .withBean(ReaderRepository.class, () -> mock(ReaderRepository.class))
            .withBean(LendingService.class, () -> mock(LendingService.class))
            .withBean(RestClient.Builder.class, RestClient::builder)
            .withPropertyValues("notification.sms.gateway-url=test-url","notification.sms.username=test-user", "notification.sms.password=test-password");

    private static List<String> scheduledCronExpressions(ApplicationContext context) {
        return context.getBean(ScheduledTaskHolder.class).getScheduledTasks().stream()
                .map(ScheduledTask::getTask)
                .filter(CronTask.class::isInstance)
                .map(task -> ((CronTask) task).getExpression())
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static List<NotificationService> enabledChannels(ApplicationContext context) {
        return (List<NotificationService>) context.getBean("enabledNotificationChannels", List.class);
    }

    private static List<String> idsOf(ApplicationContext context) {
        return enabledChannels(context).stream().map(NotificationService::channel).toList();
    }

    private static String allMessages(Throwable failure) {
        StringBuilder messages = new StringBuilder();
        for (Throwable t = failure; t != null; t = t.getCause()) {
            messages.append(t.getMessage()).append(" | ");
        }
        return messages.toString();
    }

    @Test
    void shippedPropertiesEnableTheThreeInstalledChannels() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(enabledChannels(context)).hasExactlyElementsOfTypes(
                    EmailNotificationService.class, SMSNotificationService.class, WebhookNotificationService.class);
            assertThat(context.getBean(NotificationParameters.class)).isEqualTo(new NotificationParameters(3));
        });
    }

    @Test
    void onlyTheListedChannelsAreCreated() {
        runner.withPropertyValues("notification.channels=email,webhook")
                .run(context -> assertThat(idsOf(context)).containsExactly("email", "webhook"));
    }

    @Test
    void disabledChannelNeedsNoDeliverySettings() {
        runner.withPropertyValues("notification.channels=webhook", "notification.email.smtp.host=", "notification.email.from=",
                        "notification.sms.gateway-url=", "notification.sms.username=", "notification.sms.password=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(idsOf(context)).containsExactly("webhook");
                });
    }

    @Test
    void webhookNeedsNoSetupValueBecauseEachReaderProvidesTheUrl() {
        runner.withPropertyValues("notification.channels=webhook")
                .run(context -> assertThat(enabledChannels(context)).hasExactlyElementsOfTypes(WebhookNotificationService.class));
    }

    @Test
    void smtpCredentialsAreOptional() {
        runner.withPropertyValues("notification.channels=email", "notification.email.smtp.username=library",
                        "notification.email.smtp.password=secret")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(idsOf(context)).containsExactly("email");
                });
    }

    @Test
    void unknownIdentifierStopsStartupNamingIt() {
        runner.withPropertyValues("notification.channels=email,fax")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(allMessages(context.getStartupFailure())).contains("Unknown notification channel 'fax'");
                });
    }

    @Test
    void emptyListStopsStartup() {
        runner.withPropertyValues("notification.channels=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(allMessages(context.getStartupFailure()))
                            .contains("At least one notification channel must be enabled");
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"notification.email.smtp.host", "notification.email.smtp.port", "notification.email.from",
            "notification.sms.gateway-url", "notification.sms.username", "notification.sms.password",
            "notification.sms.country-code"})
    void missingSettingOfAnEnabledChannelStopsStartupNamingTheProperty(String key) {
        runner.withPropertyValues(key + "=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(allMessages(context.getStartupFailure()))
                            .contains("Missing notification parameter '" + key + "'");
                });
    }

    @Test
    void invalidSmtpPortOrCountryCodeStopsStartup() {
        runner.withPropertyValues("notification.email.smtp.port=smtp")
                .run(context -> assertThat(allMessages(context.getStartupFailure()))
                        .contains("notification.email.smtp.port").contains("not an integer"));
        runner.withPropertyValues("notification.sms.country-code=351")
                .run(context -> assertThat(allMessages(context.getStartupFailure())).contains("country code"));
    }

    @Test
    void invalidNoticeDaysStopStartupNamingTheProblem() {
        runner.withPropertyValues("notification.due-date-notice-days=0")
                .run(context -> assertThat(allMessages(context.getStartupFailure())).contains("DueDateNoticeDays=0"));
        runner.withPropertyValues("notification.due-date-notice-days=three")
                .run(context -> assertThat(allMessages(context.getStartupFailure()))
                        .contains("notification.due-date-notice-days").contains("not an integer"));
    }

    @Test
    void periodicCheckIsScheduledWithTheConfiguredExpression() {
        runner.run(context -> assertThat(scheduledCronExpressions(context)).containsExactly("0 0 9 * * *"));
        runner.withPropertyValues("notification.check.cron=0 30 7 * * *")
                .run(context -> assertThat(scheduledCronExpressions(context)).containsExactly("0 30 7 * * *"));
    }

    @Test
    void dashDisablesThePeriodicCheck() {
        runner.withPropertyValues("notification.check.cron=-")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(scheduledCronExpressions(context)).isEmpty();
                });
    }

    @Test
    void malformedScheduleStopsStartup() {
        runner.withPropertyValues("notification.check.cron=every morning")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void externalValueOverridesShippedValueWithoutCodeChange() {
        runner.withPropertyValues("notification.due-date-notice-days=5")
                .run(context -> assertThat(context.getBean(NotificationParameters.class).dueDateNoticeDays()).isEqualTo(5));
    }
}
