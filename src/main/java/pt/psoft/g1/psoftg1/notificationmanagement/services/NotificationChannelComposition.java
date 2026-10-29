package pt.psoft.g1.psoftg1.notificationmanagement.services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Turns the configured list of channel identifiers into the enabled channel instances (CFG-NOT-01).
 * Only the listed channels are instantiated, so disabled channels send 0 notifications (QAS5).
 * Free of Spring, so the selection rules are tested without starting a context.
 */
public class NotificationChannelComposition {
    private final Map<String, Supplier<NotificationService>> installed;

    /** @param installed installed channels by identifier; a supplier runs only when its channel is enabled. */
    public NotificationChannelComposition(Map<String, Supplier<NotificationService>> installed) {
        this.installed = new LinkedHashMap<>(installed);
    }

    /**
     * @param identifiers comma-separated channel identifiers, as written in the setup property.
     * @return the enabled channels, in the configured order.
     * @throws IllegalStateException when the list is empty or has an unknown or repeated identifier; startup stops.
     */
    public List<NotificationService> enabledChannels(String identifiers) {
        // At least one channel is required: readers must always have a channel to choose (group decision).
        if (identifiers == null || identifiers.isBlank()) {
            throw new IllegalStateException("At least one notification channel must be enabled; installed: "
                    + String.join(", ", installed.keySet()));
        }

        final Set<String> selected = new LinkedHashSet<>();
        for (String raw : identifiers.split(",")) {
            final String id = raw.trim();
            if (!installed.containsKey(id)) {
                throw new IllegalStateException("Unknown notification channel '" + id + "'; installed: "
                        + String.join(", ", installed.keySet()));
            }
            if (!selected.add(id)) {
                throw new IllegalStateException("Notification channel '" + id + "' is listed more than once");
            }
        }

        final List<NotificationService> channels = new ArrayList<>();
        for (String id : selected) {
            channels.add(installed.get(id).get());
        }
        return List.copyOf(channels);
    }
}
