package pt.psoft.g1.psoftg1.bibliographicmanagement.services;

import pt.psoft.g1.psoftg1.bookmanagement.model.Isbn;

import java.util.*;

public class BibliographicService {
    private final List<BibliographicSource> sources;
    private final BibliographicMerge merge;

    public BibliographicService(List<BibliographicSource> sources, BibliographicMerge merge) {
        if (sources.isEmpty())
            throw new IllegalArgumentException("At least one source is required");
        this.sources = List.copyOf(sources);
        this.merge = merge;
    }

    public LookupResult retrieve(String isbn) {
        new Isbn(isbn); // Preserve the catalogue ISBN contract before any network request.
        return merge.combine(
                isbn, sources.stream().map(source -> source.lookupByIsbn(isbn)).toList());
    }
}
