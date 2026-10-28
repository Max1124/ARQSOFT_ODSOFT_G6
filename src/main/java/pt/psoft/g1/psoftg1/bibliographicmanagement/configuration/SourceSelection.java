package pt.psoft.g1.psoftg1.bibliographicmanagement.configuration;

import pt.psoft.g1.psoftg1.bibliographicmanagement.services.*;

import java.util.*;

public class SourceSelection {
    public List<BibliographicSource> select(String configured, List<SourceRegistration> installed) {
        Map<String, BibliographicSource> registry = new HashMap<>();
        for (var entry : installed)
            if (registry.putIfAbsent(entry.id(), entry.source()) != null)
                throw new IllegalArgumentException("Duplicate source registration: " + entry.id());
        if (configured == null || configured.isBlank())
            throw new IllegalArgumentException("bibliographic.sources must not be empty");
        Set<String> seen = new HashSet<>();
        List<BibliographicSource> selected = new ArrayList<>();
        for (String token : configured.split(",", -1)) {
            String id = token.strip();
            if (!registry.containsKey(id) || !seen.add(id))
                throw new IllegalArgumentException(
                        "Unknown or repeated bibliographic source: " + id);
            selected.add(registry.get(id));
        }
        return List.copyOf(selected);
    }
}
