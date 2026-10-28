package pt.psoft.g1.psoftg1.bibliographicmanagement.infrastructure;

import pt.psoft.g1.psoftg1.bibliographicmanagement.services.*;

import java.io.IOException;
import java.net.URI;

public class OpenLibraryAdapter implements BibliographicSource {
    private final BibliographicTransport transport;
    private final OpenLibraryMapper mapper;
    private final String endpoint;

    public OpenLibraryAdapter(
            BibliographicTransport transport, OpenLibraryMapper mapper, String endpoint) {
        this.transport = transport;
        this.mapper = mapper;
        this.endpoint = endpoint;
    }

    public SourceOutcome lookupByIsbn(String isbn) {
        try {
            var response =
                    transport.execute(
                            URI.create(
                                    endpoint
                                            + "?bibkeys=ISBN:"
                                            + isbn
                                            + "&format=json&jscmd=data"));
            if (response.status() != 200)
                return SourceOutcome.failure("open-library", "HTTP " + response.status());
            return mapper.map(isbn, response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SourceOutcome.failure("open-library", "Interrupted");
        } catch (IOException | IllegalArgumentException e) {
            return SourceOutcome.failure(
                    "open-library", "Unavailable or invalid provider response");
        }
    }
}
