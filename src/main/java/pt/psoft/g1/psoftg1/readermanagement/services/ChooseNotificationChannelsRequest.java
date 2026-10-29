package pt.psoft.g1.psoftg1.readermanagement.services;

import java.util.List;

/**
 * Channels a reader chooses to be notified through (US8), and optionally the URL that receives their webhook
 * notifications. Without webhookUrl the stored URL is kept.
 */
public record ChooseNotificationChannelsRequest(List<String> channels, String webhookUrl) {}
