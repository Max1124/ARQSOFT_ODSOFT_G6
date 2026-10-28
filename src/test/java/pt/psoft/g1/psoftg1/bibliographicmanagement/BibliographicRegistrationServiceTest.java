package pt.psoft.g1.psoftg1.bibliographicmanagement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.*;

import pt.psoft.g1.psoftg1.bibliographicmanagement.services.*;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.bookmanagement.services.*;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;

import java.util.*;

class BibliographicRegistrationServiceTest {
    BibliographicService lookup;
    CatalogueMatcher matcher;
    BookService books;
    BookRepository repository;
    BibliographicRegistrationService sut;
    static final String ISBN = "9780132350884";

    @BeforeEach
    void setup() {
        lookup = mock(BibliographicService.class);
        matcher = mock(CatalogueMatcher.class);
        books = mock(BookService.class);
        repository = mock(BookRepository.class);
        sut = new BibliographicRegistrationService(lookup, matcher, books, repository);
    }

    @Test
    void duplicateStopsBeforeProviderCalls() {
        // Arrange
        when(repository.findByIsbn(ISBN)).thenReturn(Optional.of(mock(Book.class)));
        // Act / Assert
        assertThrows(
                ConflictException.class, () -> sut.register(ISBN, RegistrationRequest.empty()));
        verifyNoInteractions(lookup, matcher, books);
    }

    @Test
    void missingOrOversizedTitleNeverWritesBook() {
        for (String title : Arrays.asList(null, " ", "a".repeat(129))) {
            // Arrange
            var data = new BibliographicData(ISBN, title, null, List.of(), List.of(), Map.of());
            when(lookup.retrieve(ISBN))
                    .thenReturn(
                            new LookupResult(
                                    LookupResult.Status.FOUND, data, List.of(), List.of()));
            var request = RegistrationRequest.empty();
            when(matcher.resolve(data, request))
                    .thenReturn(
                            new CatalogueResolution(List.of(1L), "Fiction", Map.of(), List.of()));
            // Act
            var result = sut.register(ISBN, request);
            // Assert
            assertNull(result.book());
            assertTrue(result.needs().contains("Provide a title of 1..128 characters"));
            verifyNoInteractions(books);
        }
    }

    @Test
    void manualOverridesAndResolvedIdsArePassedToExistingRegistration() {
        // Arrange
        var data =
                new BibliographicData(
                        ISBN, "External", "External description", List.of(), List.of(), Map.of());
        var request = new RegistrationRequest("Corrected", "", "Fiction", List.of(1L));
        when(lookup.retrieve(ISBN))
                .thenReturn(
                        new LookupResult(LookupResult.Status.FOUND, data, List.of(), List.of()));
        when(matcher.resolve(data, request))
                .thenReturn(new CatalogueResolution(List.of(1L), "Fiction", Map.of(), List.of()));
        var book = mock(Book.class);
        when(books.create(any(), eq(ISBN))).thenReturn(book);
        // Act
        var result = sut.register(ISBN, request);
        // Assert
        assertSame(book, result.book());
        var argument = org.mockito.ArgumentCaptor.forClass(CreateBookRequest.class);
        verify(books).create(argument.capture(), eq(ISBN));
        assertEquals("Corrected", argument.getValue().getTitle());
        assertEquals("", argument.getValue().getDescription());
        assertEquals(List.of(1L), argument.getValue().getAuthors());
        assertNull(argument.getValue().getPhoto());
    }

    @Test
    void authorNotPresentInSourceCannotBeConfirmed() {
        // Arrange
        when(lookup.retrieve(ISBN))
                .thenReturn(
                        new LookupResult(
                                LookupResult.Status.NOT_FOUND,
                                null,
                                List.of(SourceOutcome.notFound("google-books")),
                                List.of()));
        // Act / Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> sut.confirm(ISBN, "google-books", "Invented", 1L));
        verifyNoInteractions(matcher);
    }
}
