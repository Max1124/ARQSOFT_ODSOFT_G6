package pt.psoft.g1.psoftg1.bibliographicmanagement.services;

import java.util.*;

public class BibliographicMerge {
    public LookupResult combine(String isbn, List<SourceOutcome> results) {
        String title = null, description = null;
        List<String> authors = List.of();
        Map<String, String> genres = new LinkedHashMap<>(), provenance = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        boolean found = false, failed = false;
        for (var outcome : results) {
            if (outcome.status() == SourceOutcome.Status.FAILURE) {
                failed = true;
                warnings.add("Source " + outcome.source() + " failed; lookup incomplete");
                continue;
            }
            if (outcome.status() != SourceOutcome.Status.FOUND) continue;
            found = true;
            var data = outcome.data();
            if (title == null && present(data.title())) {
                title = data.title();
                provenance.put("title", outcome.source());
            }
            if (description == null && present(data.description())) {
                description = data.description();
                provenance.put("description", outcome.source());
            }
            if (authors.isEmpty() && !data.authors().isEmpty()) {
                authors = data.authors();
                provenance.put("authors", outcome.source());
            } else if (!data.authors().isEmpty()
                    && !normalized(authors).equals(normalized(data.authors())))
                warnings.add(
                        "Author list differs in "
                                + outcome.source()
                                + "; first available list retained");
            for (String genre : data.genres()) {
                String key = normalize(genre);
                if (!key.isEmpty() && !genres.containsKey(key)) {
                    genres.put(key, genre.strip());
                    provenance.put("genre:" + genre.strip(), outcome.source());
                }
            }
        }
        var status =
                found
                        ? LookupResult.Status.FOUND
                        : failed ? LookupResult.Status.FAILURE : LookupResult.Status.NOT_FOUND;
        var data =
                found
                        ? new BibliographicData(
                                isbn,
                                title,
                                description,
                                authors,
                                new ArrayList<>(genres.values()),
                                provenance)
                        : null;
        return new LookupResult(status, data, results, warnings);
    }

    private boolean present(String s) {
        return s != null && !s.isBlank();
    }

    private String normalize(String s) {
        return s.strip().toLowerCase(Locale.ROOT);
    }

    private List<String> normalized(List<String> values) {
        return values.stream().map(this::normalize).toList();
    }
}
