package pt.psoft.g1.psoftg1.bibliographicmanagement.infrastructure;

import pt.psoft.g1.psoftg1.bibliographicmanagement.services.*;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class    GoogleBooksAdapter implements BibliographicSource {
    private final BibliographicTransport transport;
    private final GoogleBooksMapper mapper;
    private final String endpoint;
    private final String apiKey;

    public GoogleBooksAdapter(
            BibliographicTransport transport, GoogleBooksMapper mapper, String endpoint) {
        this(transport, mapper, endpoint, "");
    }

    public GoogleBooksAdapter(
            BibliographicTransport transport, GoogleBooksMapper mapper, String endpoint, String apiKey) {
        this.transport = transport;
        this.mapper = mapper;
        this.endpoint = endpoint;
        this.apiKey = apiKey == null ? "" : apiKey.strip();
    }

    public SourceOutcome lookupByIsbn(String isbn) {
        try {
            String query = endpoint + "?q=isbn:" + isbn + "&maxResults=40";
            if (!apiKey.isEmpty()) {
                query += "&key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            }
            var response =
                    transport.execute(URI.create(query));
            if (response.status() != 200)
                return SourceOutcome.failure("google-books", "HTTP " + response.status());
            return mapper.map(isbn, response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SourceOutcome.failure("google-books", "Interrupted");
        } catch (IOException | IllegalArgumentException e) {
            return SourceOutcome.failure(
                    "google-books", "Unavailable or invalid provider response");
        }
    }
}
