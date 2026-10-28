package pt.psoft.g1.psoftg1.bibliographicmanagement.services;

public record SourceOutcome(String source, Status status, BibliographicData data, String reason) {
    public enum Status {
        FOUND,
        NOT_FOUND,
        FAILURE
    }

    public SourceOutcome {
        if (source == null
                || source.isBlank()
                || status == null
                || (status == Status.FOUND) != (data != null))
            throw new IllegalArgumentException("Invalid source outcome");
    }

    public static SourceOutcome found(String source, BibliographicData data) {
        return new SourceOutcome(source, Status.FOUND, data, null);
    }

    public static SourceOutcome notFound(String source) {
        return new SourceOutcome(source, Status.NOT_FOUND, null, null);
    }

    public static SourceOutcome failure(String source, String reason) {
        return new SourceOutcome(source, Status.FAILURE, null, reason);
    }
}
