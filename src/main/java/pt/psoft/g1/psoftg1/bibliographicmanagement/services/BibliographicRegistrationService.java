package pt.psoft.g1.psoftg1.bibliographicmanagement.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.bookmanagement.services.*;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;

import java.util.*;

@Service
public class BibliographicRegistrationService {
    private final BibliographicService lookup;
    private final CatalogueMatcher matcher;
    private final BookService books;
    private final BookRepository repository;

    public BibliographicRegistrationService(
            BibliographicService lookup,
            CatalogueMatcher matcher,
            BookService books,
            BookRepository repository) {
        this.lookup = lookup;
        this.matcher = matcher;
        this.books = books;
        this.repository = repository;
    }

    public record Preview(LookupResult lookup, CatalogueResolution catalogue) {}

    public record Registration(Preview preview, Book book, List<String> needs) {}

    public Preview preview(String isbn) {
        var result = lookup.retrieve(isbn);
        return new Preview(result, matcher.resolve(result.data(), RegistrationRequest.empty()));
    }

    @Transactional
    public Registration register(String isbn, RegistrationRequest overrides) {
        if (repository.findByIsbn(isbn).isPresent())
            throw new ConflictException("Book with ISBN already exists");
        var result = lookup.retrieve(isbn);
        var data = result.data();
        var catalogue = matcher.resolve(data, overrides);
        List<String> needs = new ArrayList<>(catalogue.needs());
        String title =
                overrides.title() != null ? overrides.title() : data == null ? null : data.title();
        String description =
                overrides.description() != null
                        ? overrides.description()
                        : data == null ? null : data.description();
        if (title == null || title.isBlank() || title.length() > 128)
            needs.add("Provide a title of 1..128 characters");
        if (description != null && description.length() > 4096)
            needs.add("Description must not exceed 4096 characters");
        var preview = new Preview(result, catalogue);
        if (!needs.isEmpty()) return new Registration(preview, null, List.copyOf(needs));
        var request = new CreateBookRequest();
        request.setTitle(title);
        request.setDescription(description == null ? "" : description);
        request.setGenre(catalogue.genre());
        request.setAuthors(catalogue.authorIds());
        return new Registration(preview, books.create(request, isbn), List.of());
    }

    @Transactional
    public void confirm(String isbn, String source, String name, Long authorId) {
        var result = lookup.retrieve(isbn);
        boolean present =
                result.sources().stream()
                        .anyMatch(
                                outcome ->
                                        outcome.source().equals(source)
                                                && outcome.status() == SourceOutcome.Status.FOUND
                                                && outcome.data().authors().contains(name));
        if (!present)
            throw new IllegalArgumentException("Author must occur in this source's ISBN response");
        matcher.confirm(source, isbn, name, authorId);
    }
}
