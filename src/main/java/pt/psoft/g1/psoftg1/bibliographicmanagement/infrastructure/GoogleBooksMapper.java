package pt.psoft.g1.psoftg1.bibliographicmanagement.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;

import pt.psoft.g1.psoftg1.bibliographicmanagement.services.*;

import java.io.IOException;
import java.util.Map;

public class GoogleBooksMapper {
    private final ObjectMapper json;

    public GoogleBooksMapper(ObjectMapper json) {
        this.json = json;
    }

    public SourceOutcome map(String isbn, String body) throws IOException {
        var root = json.readTree(body);
        if (root == null || !root.isObject() || !root.path("totalItems").isIntegralNumber())
            throw new IllegalArgumentException("Invalid Google response");
        if (root.path("totalItems").asInt() == 0) return SourceOutcome.notFound("google-books");
        if (!root.path("items").isArray() || root.path("items").isEmpty())
            throw new IllegalArgumentException("Missing Google items");
        for (var item : root.path("items")) {
            var v = item.path("volumeInfo");
            if (!v.isObject()) throw new IllegalArgumentException("Invalid volume");
            boolean matches = false;
            for (var id : v.path("industryIdentifiers"))
                if (isbn.equals(id.path("identifier").asText())) matches = true;
            if (!matches) continue; // Never register metadata for a different edition/ISBN.
            return SourceOutcome.found(
                    "google-books",
                    new BibliographicData(
                            isbn,
                            ProviderJson.text(v, "title"),
                            ProviderJson.text(v, "description"),
                            ProviderJson.strings(v, "authors", null),
                            ProviderJson.strings(v, "categories", null),
                            Map.of()));
        }
        return SourceOutcome.notFound("google-books");
    }
}
