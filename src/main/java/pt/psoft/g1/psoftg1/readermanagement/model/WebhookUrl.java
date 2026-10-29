package pt.psoft.g1.psoftg1.readermanagement.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.net.URI;
import java.net.URISyntaxException;

/** Target of the reader's webhook notifications: an absolute http or https URL. */
@Embeddable
public class WebhookUrl {
    @Column(name = "NOTIFICATION_WEBHOOK_URL", length = 2048)
    String webhookUrl;

    public WebhookUrl(String webhookUrl) {
        setWebhookUrl(webhookUrl);
    }

    protected WebhookUrl() {}

    private void setWebhookUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Webhook URL must not be empty");
        }
        final URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Webhook URL is not valid: " + url);
        }
        final String scheme = uri.getScheme();
        if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) || uri.getHost() == null) {
            throw new IllegalArgumentException("Webhook URL is not valid: " + url);
        }

        this.webhookUrl = uri.toString();
    }

    public String toString() {
        return this.webhookUrl;
    }
}
