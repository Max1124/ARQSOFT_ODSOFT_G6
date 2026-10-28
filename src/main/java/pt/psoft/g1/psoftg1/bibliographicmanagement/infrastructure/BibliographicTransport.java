package pt.psoft.g1.psoftg1.bibliographicmanagement.infrastructure;

import java.io.IOException;
import java.net.URI;

public interface BibliographicTransport {
    TransportResponse execute(URI uri) throws IOException, InterruptedException;

    record TransportResponse(int status, String body) {}
}
