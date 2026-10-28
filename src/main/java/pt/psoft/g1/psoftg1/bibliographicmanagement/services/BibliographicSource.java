package pt.psoft.g1.psoftg1.bibliographicmanagement.services;

public interface BibliographicSource {
    SourceOutcome lookupByIsbn(String isbn);
}
