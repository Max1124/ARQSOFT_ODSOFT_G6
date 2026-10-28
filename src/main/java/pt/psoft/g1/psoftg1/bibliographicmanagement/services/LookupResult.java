package pt.psoft.g1.psoftg1.bibliographicmanagement.services;

import java.util.*;

public record LookupResult(
        Status status, BibliographicData data, List<SourceOutcome> sources, List<String> warnings) {
    public enum Status {
        FOUND,
        NOT_FOUND,
        FAILURE
    }

    public LookupResult {
        sources = List.copyOf(sources);
        warnings = List.copyOf(warnings);
    }
}
