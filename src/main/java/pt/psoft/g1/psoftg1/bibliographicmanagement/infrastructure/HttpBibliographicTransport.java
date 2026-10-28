package pt.psoft.g1.psoftg1.bibliographicmanagement.infrastructure;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;

public class HttpBibliographicTransport implements BibliographicTransport {
    private final HttpClient client;
    private final Duration timeout;

    public HttpBibliographicTransport(HttpClient client, Duration timeout) {
        this.client = client;
        this.timeout = timeout;
    }

    public TransportResponse execute(URI uri) throws IOException, InterruptedException {
        var request =
                HttpRequest.newBuilder(uri)
                        .timeout(timeout)
                        .header("Accept", "application/json")
                        .header("User-Agent", "LMS-Bibliographic/1.0")
                        .GET()
                        .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return new TransportResponse(response.statusCode(), response.body());
    }
}
