package pt.psoft.g1.psoftg1.bibliographicmanagement.services;

public record SourceRegistration(String id, BibliographicSource source) {
    public SourceRegistration {
        if (id == null || id.isBlank() || source == null)
            throw new IllegalArgumentException("Invalid source registration");
    }
}
