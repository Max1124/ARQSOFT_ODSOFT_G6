package pt.psoft.g1.psoftg1.bibliographicmanagement.services;

import java.util.*;

public record CatalogueResolution(
        List<Long> authorIds,
        String genre,
        Map<String, List<Long>> authorSuggestions,
        List<String> needs) {
    public CatalogueResolution {
        authorIds = List.copyOf(authorIds);
        authorSuggestions = Map.copyOf(authorSuggestions);
        needs = List.copyOf(needs);
    }
}
