package pt.psoft.g1.psoftg1.bibliographicmanagement.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;

import pt.psoft.g1.psoftg1.bibliographicmanagement.services.*;

import java.io.IOException;
import java.util.Map;

public class OpenLibraryMapper {
    private final ObjectMapper json;

    public OpenLibraryMapper(ObjectMapper json) {
        this.json = json;
    }

    public SourceOutcome map(String isbn, String body) throws IOException {
        var root = json.readTree(body);
        if (root == null || !root.isObject())
            throw new IllegalArgumentException("Invalid Open Library response");
        var v = root.get("ISBN:" + isbn);
        if (v == null) return SourceOutcome.notFound("open-library");
        if (!v.isObject()) throw new IllegalArgumentException("Invalid Open Library book");
        // The data endpoint may omit description. Missing metadata is not a failed lookup.
        String description = null;
        if (v.has("description"))
            description =
                    v.get("description").isObject()
                            ? ProviderJson.text(v.get("description"), "value")
                            : ProviderJson.text(v, "description");
        return SourceOutcome.found(
                "open-library",
                new BibliographicData(
                        isbn,
                        ProviderJson.text(v, "title"),
                        description,
                        ProviderJson.strings(v, "authors", "name"),
                        ProviderJson.strings(v, "subjects", "name"),
                        Map.of()));
    }
}
