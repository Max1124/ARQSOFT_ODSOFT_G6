package pt.psoft.g1.psoftg1.bibliographicmanagement.api;

import jakarta.annotation.security.RolesAllowed;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import pt.psoft.g1.psoftg1.bibliographicmanagement.services.*;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookView;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookViewMapper;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/bibliographic")
@RolesAllowed("LIBRARIAN")
public class BibliographicController {
    private final BibliographicRegistrationService service;
    private final BookViewMapper mapper;

    public BibliographicController(
            BibliographicRegistrationService service, @Qualifier("bookViewMapperImpl") BookViewMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping("/{isbn}")
    public BibliographicRegistrationService.Preview lookup(@PathVariable String isbn) {
        return service.preview(isbn);
    }

    public record RegistrationResponse(
            BibliographicRegistrationService.Preview preview, BookView book, List<String> needs) {}

    @PostMapping("/{isbn}/registration")
    public ResponseEntity<RegistrationResponse> register(
            @PathVariable String isbn, @RequestBody(required = false) RegistrationRequest request) {
        var result =
                service.register(isbn, request == null ? RegistrationRequest.empty() : request);
        if (result.book() == null)
            return ResponseEntity.unprocessableEntity()
                    .body(new RegistrationResponse(result.preview(), null, result.needs()));
        return ResponseEntity.created(URI.create("/api/books/" + isbn))
                .body(
                        new RegistrationResponse(
                                result.preview(), mapper.toBookView(result.book()), List.of()));
    }

    public record Confirmation(String source, String externalName, Long authorId) {}

    @PutMapping("/{isbn}/author-confirmations")
    public ResponseEntity<Void> confirm(
            @PathVariable String isbn, @RequestBody Confirmation request) {
        service.confirm(isbn, request.source(), request.externalName(), request.authorId());
        return ResponseEntity.noContent().build();
    }
}
