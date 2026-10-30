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
        assertEquals("Fiction", argument.getValue().getGenre());
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

    @Test
    void confirmedAuthorMustComeFromTheRequestedSource() {
        // Arrange
        var data = new BibliographicData(ISBN,"Title",null,List.of("Alice"),List.of(),Map.of());
        when(lookup.retrieve(ISBN)).thenReturn(new LookupResult(LookupResult.Status.FOUND,data,
                List.of(SourceOutcome.found("google-books",data)),List.of()));
        // Act / Assert
        sut.confirm(ISBN,"google-books","Alice",7L);
        verify(matcher).confirm("google-books",ISBN,"Alice",7L);
        assertThrows(IllegalArgumentException.class,()->sut.confirm(ISBN,"open-library","Alice",7L));
        assertThrows(IllegalArgumentException.class,()->sut.confirm(ISBN,"google-books","Other",7L));
        verifyNoMoreInteractions(matcher);
    }

    @Test
    void previewReturnsLookupAndResolutionWithoutWriting() {
        // Arrange
        var result = new LookupResult(LookupResult.Status.NOT_FOUND,null,List.of(),List.of());
        var resolution = new CatalogueResolution(List.of(),null,Map.of(),List.of("Select catalogue authors"));
        when(lookup.retrieve(ISBN)).thenReturn(result);
        when(matcher.resolve(null,RegistrationRequest.empty())).thenReturn(resolution);
        // Act
        var preview=sut.preview(ISBN);
        // Assert
        assertSame(result,preview.lookup()); assertSame(resolution,preview.catalogue());
        verifyNoInteractions(books,repository);
    }

    @Test
    void manualRegistrationWorksWithoutProviderDataAndRespectsLengthLimits() {
        // Arrange
        when(lookup.retrieve(ISBN)).thenReturn(new LookupResult(LookupResult.Status.FAILURE,null,List.of(),List.of()));
        when(matcher.resolve(eq(null),any())).thenReturn(new CatalogueResolution(List.of(7L),"Computers",Map.of(),List.of()));
        var book=mock(Book.class); when(books.create(any(),eq(ISBN))).thenReturn(book);
        var request=new RegistrationRequest("T".repeat(128),"D".repeat(4096),"Computers",List.of(7L));
        // Act
        assertSame(book,sut.register(ISBN,request).book());
        // Assert
        var captured=org.mockito.ArgumentCaptor.forClass(CreateBookRequest.class);
        verify(books).create(captured.capture(),eq(ISBN));
        assertEquals(request.title(),captured.getValue().getTitle());
        assertEquals(request.description(),captured.getValue().getDescription());
        clearInvocations(books);
        var rejected=sut.register(ISBN,new RegistrationRequest("Title","D".repeat(4097),"Computers",List.of(7L)));
        assertNull(rejected.book()); assertTrue(rejected.needs().contains("Description must not exceed 4096 characters"));
        verifyNoInteractions(books);
    }
}
