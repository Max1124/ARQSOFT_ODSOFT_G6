package pt.psoft.g1.psoftg1.bibliographicmanagement.infrastructure;

import pt.psoft.g1.psoftg1.bibliographicmanagement.services.*;

import java.io.IOException;
import java.net.URI;

public class    GoogleBooksAdapter implements BibliographicSource {
    private final BibliographicTransport transport;
    private final GoogleBooksMapper mapper;
    private final String endpoint;

    public GoogleBooksAdapter(
            BibliographicTransport transport, GoogleBooksMapper mapper, String endpoint) {
        this.transport = transport;
        this.mapper = mapper;
        this.endpoint = endpoint;
    }

    public SourceOutcome lookupByIsbn(String isbn) {
        try {
            var response =
                    transport.execute(URI.create(endpoint + "?q=isbn:" + isbn + "&maxResults=40"));
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
