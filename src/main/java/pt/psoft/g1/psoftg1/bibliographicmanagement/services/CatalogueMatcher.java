package pt.psoft.g1.psoftg1.bibliographicmanagement.services;

import org.springframework.stereotype.Service;

import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bibliographicmanagement.model.AuthorCorrespondence;
import pt.psoft.g1.psoftg1.bibliographicmanagement.repositories.AuthorCorrespondenceRepository;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;

import java.util.*;

@Service
public class CatalogueMatcher {
    private final AuthorRepository authors;
    private final GenreRepository genres;
    private final AuthorCorrespondenceRepository confirmations;

    public CatalogueMatcher(
            AuthorRepository authors,
            GenreRepository genres,
            AuthorCorrespondenceRepository confirmations) {
        this.authors = authors;
        this.genres = genres;
        this.confirmations = confirmations;
    }

    public CatalogueResolution resolve(BibliographicData data, RegistrationRequest request) {
        List<String> needs = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        Map<String, List<Long>> suggestions = new LinkedHashMap<>();
        if (request.authorIds() != null) {
            for (Long id : request.authorIds()) {
                if (id == null || authors.findByAuthorNumber(id).isEmpty())
                    needs.add("Unknown author ID: " + id);
                else if (!ids.contains(id)) ids.add(id);
            }
            if (ids.isEmpty()) needs.add("Select at least one catalogue author");
        } else if (data == null || data.authors().isEmpty()) needs.add("Select catalogue authors");
        else {
            for (String name : data.authors()) {
                String source = data.provenance().get("authors");
                var confirmation =
                        source == null
                                ? Optional.<AuthorCorrespondence>empty()
                                : confirmations.findById(
                                        AuthorCorrespondence.key(source, data.isbn(), name));
                if (confirmation.isPresent()
                        && authors.findByAuthorNumber(confirmation.get().getAuthorId())
                                .isPresent()) {
                    Long id = confirmation.get().getAuthorId();
                    if (!ids.contains(id)) ids.add(id);
                } else {
                    List<Long> candidates = new ArrayList<>();
                    for (var author : authors.findAll())
                        if (normalize(author.getName()).equals(normalize(name)))
                            candidates.add(author.getAuthorNumber());
                    suggestions.put(name, List.copyOf(candidates));
                    needs.add("Confirm catalogue author: " + name);
                }
            }
        }
        String genre = null;
        if (request.genre() != null) {
            var existing = genres.findByString(request.genre());
            if (existing.isPresent()) genre = existing.get().getGenre();
            else needs.add("Select an existing catalogue genre");
        } else {
            Set<String> matches = new LinkedHashSet<>();
            int matchingEntries = 0;
            if (data != null)
                for (var candidate : genres.findAll())
                    if (data.genres().stream()
                            .anyMatch(
                                    value ->
                                            normalize(value)
                                                    .equals(normalize(candidate.getGenre())))) {
                        matches.add(candidate.getGenre());
                        matchingEntries++;
                    }
            if (matchingEntries == 1) genre = matches.iterator().next();
            else needs.add("Select one catalogue genre");
        }
        return new CatalogueResolution(ids, genre, suggestions, needs);
    }

    public void confirm(String source, String isbn, String name, Long id) {
        if (id == null || authors.findByAuthorNumber(id).isEmpty())
            throw new IllegalArgumentException("Unknown catalogue author");
        confirmations.save(new AuthorCorrespondence(source, isbn, name, id));
    }

    private String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }
}
