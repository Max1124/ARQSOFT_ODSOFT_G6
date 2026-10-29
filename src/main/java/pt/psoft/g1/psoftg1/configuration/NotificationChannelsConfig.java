package pt.psoft.g1.psoftg1.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.web.client.RestClient;
import pt.psoft.g1.psoftg1.lendingmanagement.services.LendingService;
import pt.psoft.g1.psoftg1.notificationmanagement.services.EmailNotificationService;
import pt.psoft.g1.psoftg1.notificationmanagement.services.NotificationChannelComposition;
import pt.psoft.g1.psoftg1.notificationmanagement.services.NotificationParameters;
import pt.psoft.g1.psoftg1.notificationmanagement.services.NotificationService;
import pt.psoft.g1.psoftg1.notificationmanagement.services.SMSNotificationService;
import pt.psoft.g1.psoftg1.notificationmanagement.services.WebhookNotificationService;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Setup composition of the notification channels (EXT-NOT-05, CFG-NOT-01): registers the installed channels,
 * instantiates only those listed in {@code notification.channels} and exposes them as {@code enabledNotificationChannels}.
 * An empty, unknown or repeated identifier, or a missing value of an enabled channel, stops startup.
 * It also schedules the periodic due-date and overdue check of LendingServiceImpl.
 */
@Configuration
@EnableScheduling
@PropertySource({"classpath:config/library.properties"})
public class NotificationChannelsConfig {

    // Registration of the installed channels. A new channel adds one entry here; the lending service,
    // the composition and the existing channels do not change (QAS2).
    // Each setting is read inside its supplier, so a disabled channel needs no configuration.
    // The webhook target is provided by each reader, so that channel has no setup value.
    @Bean
    public NotificationChannelComposition notificationChannelComposition(ReaderRepository readerRepository,
                                                                         RestClient.Builder http,
                                                                         Environment env) {
        final Map<String, Supplier<NotificationService>> installed = new LinkedHashMap<>();
        installed.put(EmailNotificationService.CHANNEL, () -> new EmailNotificationService(
                readerRepository, smtpSender(env), required(env, "notification.email.from")));
        installed.put(SMSNotificationService.CHANNEL, () -> new SMSNotificationService(
                readerRepository, http.clone(), required(env, "notification.sms.gateway-url"),
                required(env, "notification.sms.username"), required(env, "notification.sms.password"),
                required(env, "notification.sms.country-code")));
        installed.put(WebhookNotificationService.CHANNEL, () -> new WebhookNotificationService(
                readerRepository, http.clone()));
        return new NotificationChannelComposition(installed);
    }

    // SMTP server of the email channel. Username and password are optional (a relay may not require them);
    // when a username is set, SMTP authentication is used. STARTTLS is used when the server offers it.
    private static JavaMailSender smtpSender(Environment env) {
        final JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(required(env, "notification.email.smtp.host"));
        sender.setPort(integer(env, "notification.email.smtp.port"));
        final String username = env.getProperty("notification.email.smtp.username");
        if (username != null && !username.isBlank()) {
            sender.setUsername(username.trim());
            sender.setPassword(env.getProperty("notification.email.smtp.password"));
            sender.getJavaMailProperties().put("mail.smtp.auth", "true");
        }
        sender.getJavaMailProperties().put("mail.smtp.starttls.enable", "true");
        return sender;
    }

    @Bean
    public List<NotificationService> enabledNotificationChannels(NotificationChannelComposition composition,
                                                                 Environment env) {
        return composition.enabledChannels(env.getProperty("notification.channels"));
    }

    // The schedule of the periodic check is bound here, so LendingServiceImpl reads no property (CFG tactics:
    // Use an Intermediary). The value "-" disables the check; a malformed expression stops startup.
    @Bean
    public SchedulingConfigurer notificationCheckSchedule(LendingService lendingService, Environment env) {
        final String cron = required(env, "notification.check.cron");
        return registrar -> registrar.addCronTask(lendingService::checkDueAndOverdueLendings, cron);
    }

    @Bean
    public NotificationParameters notificationParameters(Environment env) {
        return new NotificationParameters(integer(env, "notification.due-date-notice-days"));
    }

    private static int integer(Environment env, String key) {
        final String value = required(env, key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid notification parameter '" + key + "': '" + value + "' is not an integer", e);
        }
    }

    // Missing values stop startup instead of silently defaulting, so the operator sees which key is wrong.
    private static String required(Environment env, String key) {
        final String value = env.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing notification parameter '" + key + "'");
        }
        return value.trim();
    }
}
