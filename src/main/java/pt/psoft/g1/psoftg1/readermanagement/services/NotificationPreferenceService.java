package pt.psoft.g1.psoftg1.readermanagement.services;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.notificationmanagement.services.NotificationService;
import pt.psoft.g1.psoftg1.notificationmanagement.services.WebhookNotificationService;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Reader preference validation and storage (CFG-NOT-02, QAS6): a reader chooses at least one channel, and only
 * among the channels enabled at setup. The choice is runtime data and applies to the next notification.
 */
@Service
public class NotificationPreferenceService {
    private final ReaderRepository readerRepository;
    private final Set<String> enabledChannels;

    // The enabled set comes from the setup composition, not from properties (CFG-NOT-01).
    public NotificationPreferenceService(ReaderRepository readerRepository,
                                         @Qualifier("enabledNotificationChannels") List<NotificationService> enabledChannels) {
        this.readerRepository = readerRepository;
        this.enabledChannels = new HashSet<>();
        for (NotificationService channel : enabledChannels) {
            this.enabledChannels.add(channel.channel());
        }
    }

    /**
     * @param webhookUrl the reader's webhook target; null keeps the stored one.
     * @throws IllegalArgumentException when the choice is empty, names a channel that is not enabled, has an invalid
     *                                  webhook URL, or chooses the webhook channel without any webhook URL; nothing is stored.
     * @throws NotFoundException        when no reader has this username.
     */
    public ReaderDetails validateAndStore(final String username, final List<String> channels, final String webhookUrl) {
        if (channels == null || channels.isEmpty()) {
            throw new IllegalArgumentException("At least one notification channel must be chosen");
        }

        final Set<String> chosen = new LinkedHashSet<>(channels);
        final List<String> disabled = chosen.stream().filter(channel -> !enabledChannels.contains(channel)).toList();
        if (!disabled.isEmpty()) {
            throw new IllegalArgumentException("Notification channel not enabled: " + String.join(", ", disabled));
        }

        final ReaderDetails readerDetails = readerRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException(ReaderDetails.class, username));
        if (webhookUrl != null) {
            readerDetails.defineNotificationWebhookUrl(webhookUrl);
        }
        // The webhook channel posts to the reader's own URL, so it cannot be chosen before one is provided.
        if (chosen.contains(WebhookNotificationService.CHANNEL) && readerDetails.getNotificationWebhookUrl().isEmpty()) {
            throw new IllegalArgumentException("The webhook channel requires a webhook URL");
        }
        readerDetails.chooseNotificationChannels(chosen);
        return readerRepository.save(readerDetails);
    }
}
