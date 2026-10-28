package pt.psoft.g1.psoftg1.bibliographicmanagement.services;

import java.util.*;

/** Partial external metadata; never catalogue entities. */
public record BibliographicData(
        String isbn,
        String title,
        String description,
        List<String> authors,
        List<String> genres,
        Map<String, String> provenance) {
    public BibliographicData {
        authors = List.copyOf(authors);
        genres = List.copyOf(genres);
        provenance = Map.copyOf(provenance);
    }
}
