package pt.psoft.g1.psoftg1.bookmanagement.services;

import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;
import pt.psoft.g1.psoftg1.shared.services.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Class-level test of {@link BookServiceImpl}: all collaborators are doubles, no Spring context (CON-TST-10). */
class BookServiceImplTest {
    private static final String ISBN = "9782826012092";

    private BookRepository bookRepository;
    private GenreRepository genreRepository;
    private AuthorRepository authorRepository;
    private PhotoRepository photoRepository;
    private ReaderRepository readerRepository;
    private BookServiceImpl service;

    private final Genre fantasy = new Genre("Fantasia");
    private final Author author1 = new Author("João Alberto", "O João Alberto nasceu em Chaves e foi pedreiro.", null);
    private final Author author2 = new Author("Maria José", "A Maria José nasceu em Viseu e come laranjas.", null);

    @BeforeEach
    void setUp() {
        bookRepository = mock(BookRepository.class);
        genreRepository = mock(GenreRepository.class);
        authorRepository = mock(AuthorRepository.class);
        photoRepository = mock(PhotoRepository.class);
        readerRepository = mock(ReaderRepository.class);
        service = new BookServiceImpl(bookRepository, genreRepository, authorRepository, photoRepository, readerRepository);
        // Normally injected from library.properties.
        ReflectionTestUtils.setField(service, "suggestionsLimitPerGenre", 2L);

        when(bookRepository.save(any(Book.class))).thenAnswer(i -> i.getArgument(0));
        when(genreRepository.findByString("Fantasia")).thenReturn(Optional.of(fantasy));
        when(authorRepository.findByAuthorNumber(1L)).thenReturn(Optional.of(author1));
        when(authorRepository.findByAuthorNumber(2L)).thenReturn(Optional.of(author2));
    }

    private CreateBookRequest createRequest(List<Long> authors) {
        final var request = new CreateBookRequest();
        request.setTitle("Encantos de contar");
        request.setDescription("Uma descrição");
        request.setGenre("Fantasia");
        request.setAuthors(authors);
        return request;
    }

    private Book existingBook(String photoURI) {
        final var book = new Book(ISBN, "Encantos de contar", "Uma descrição", fantasy, List.of(author1), photoURI);
        ReflectionTestUtils.setField(book, "version", 0L);
        return book;
    }

    // create

    @Test
    void createSavesTheBookWithTheResolvedGenreAndAuthors() {
        final var book = service.create(createRequest(List.of(1L, 2L)), ISBN);

        assertEquals(ISBN, book.getIsbn());
        assertSame(fantasy, book.getGenre());
        assertEquals(List.of(author1, author2), book.getAuthors());
        verify(bookRepository).save(book);
    }

    @Test
    void createRejectsAnIsbnThatAlreadyExists() {
        when(bookRepository.findByIsbn(ISBN)).thenReturn(Optional.of(existingBook(null)));

        assertThrows(ConflictException.class, () -> service.create(createRequest(List.of(1L)), ISBN));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void createRejectsAnUnknownGenre() {
        final var request = createRequest(List.of(1L));
        request.setGenre("Inexistente");
        when(genreRepository.findByString("Inexistente")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.create(request, ISBN));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void createIgnoresUnknownAuthorNumbers() {
        when(authorRepository.findByAuthorNumber(99L)).thenReturn(Optional.empty());

        final var book = service.create(createRequest(List.of(99L, 1L)), ISBN);

        assertEquals(List.of(author1), book.getAuthors());
    }

    @Test
    void createWithOnlyUnknownAuthorsIsRejectedByTheDomain() {
        when(authorRepository.findByAuthorNumber(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.create(createRequest(List.of(99L)), ISBN));
        verify(bookRepository, never()).save(any());
    }

    // As-is: the URI is cleared on the request but the local copy was already read, so the book still gets the photo.
    @Test
    void createClearsAPhotoUriThatHasNoMatchingFileOnTheRequest() {
        final var request = createRequest(List.of(1L));
        request.setPhotoURI("cover.png");

        service.create(request, ISBN);

        assertNull(request.getPhotoURI());
        assertNull(request.getPhoto());
    }

    @Test
    void createKeepsThePhotoWhenFileAndUriArePresent() {
        final var request = createRequest(List.of(1L));
        request.setPhoto(mock(MultipartFile.class));
        request.setPhotoURI("cover.png");

        final var book = service.create(request, ISBN);

        assertEquals("cover.png", book.getPhoto().getPhotoFile());
    }

    // update

    @Test
    void updateOfAnUnknownBookIsNotFound() {
        when(bookRepository.findByIsbn(ISBN)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.update(new UpdateBookRequest(ISBN, "T", null, List.of(1L), null), "0"));
    }

    @Test
    void updateAppliesTheRequestAndSaves() {
        final var book = existingBook(null);
        when(bookRepository.findByIsbn(ISBN)).thenReturn(Optional.of(book));
        final var request = new UpdateBookRequest(ISBN, "Novo título", "Fantasia", List.of(2L), null);

        final var updated = service.update(request, "0");

        assertEquals("Novo título", updated.getTitle().toString());
        assertEquals(List.of(author2), updated.getAuthors());
        verify(bookRepository).save(book);
    }

    @Test
    void updateWithoutAuthorsOrGenreKeepsThem() {
        final var book = existingBook(null);
        when(bookRepository.findByIsbn(ISBN)).thenReturn(Optional.of(book));
        final var request = new UpdateBookRequest();
        request.setIsbn(ISBN);
        request.setTitle("Novo título");

        final var updated = service.update(request, "0");

        assertEquals(List.of(author1), updated.getAuthors());
        assertSame(fantasy, updated.getGenre());
        verify(genreRepository, never()).findByString(anyString());
    }

    @Test
    void updateWithUnknownGenreIsNotFound() {
        when(bookRepository.findByIsbn(ISBN)).thenReturn(Optional.of(existingBook(null)));
        when(genreRepository.findByString("Inexistente")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.update(new UpdateBookRequest(ISBN, null, "Inexistente", List.of(1L), null), "0"));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void updateWithAStaleVersionIsRejected() {
        when(bookRepository.findByIsbn(ISBN)).thenReturn(Optional.of(existingBook(null)));

        assertThrows(StaleObjectStateException.class,
                () -> service.update(new UpdateBookRequest(ISBN, "Novo título", null, List.of(1L), null), "7"));
        verify(bookRepository, never()).save(any());
    }

    // finders

    @Test
    void findByIsbnReturnsTheBook() {
        final var book = existingBook(null);
        when(bookRepository.findByIsbn(ISBN)).thenReturn(Optional.of(book));

        assertSame(book, service.findByIsbn(ISBN));
    }

    @Test
    void findByIsbnOfAnUnknownBookIsNotFound() {
        when(bookRepository.findByIsbn(ISBN)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.findByIsbn(ISBN));
    }

    @Test
    void findByAuthorNameSearchesByPrefix() {
        final var books = List.of(existingBook(null));
        when(bookRepository.findByAuthorName("João%")).thenReturn(books);

        assertSame(books, service.findByAuthorName("João"));
    }

    @Test
    void findByGenreAndByTitleDelegateToTheRepository() {
        final var books = List.of(existingBook(null));
        when(bookRepository.findByGenre("Fantasia")).thenReturn(books);
        when(bookRepository.findByTitle("Encantos")).thenReturn(books);

        assertSame(books, service.findByGenre("Fantasia"));
        assertSame(books, service.findByTitle("Encantos"));
    }

    @Test
    void findTop5AsksForTheLastYearAndFivePerPage() {
        final var dto = new BookCountDTO(existingBook(null), 3);
        when(bookRepository.findTop5BooksLent(any(LocalDate.class), eq(PageRequest.of(0, 5))))
                .thenReturn(new PageImpl<>(List.of(dto)));

        final var top = service.findTop5BooksLent();

        assertEquals(List.of(dto), top);
        verify(bookRepository).findTop5BooksLent(eq(LocalDate.now().minusYears(1)), eq(PageRequest.of(0, 5)));
    }

    // photo

    @Test
    void removePhotoOfABookWithoutPhotoIsNotFound() {
        when(bookRepository.findByIsbn(ISBN)).thenReturn(Optional.of(existingBook(null)));

        assertThrows(NotFoundException.class, () -> service.removeBookPhoto(ISBN, 0L));
        verify(photoRepository, never()).deleteByPhotoFile(anyString());
    }

    @Test
    void removePhotoClearsItSavesTheBookAndDeletesThePhotoRecord() {
        final var book = existingBook("cover.png");
        when(bookRepository.findByIsbn(ISBN)).thenReturn(Optional.of(book));

        final var updated = service.removeBookPhoto(ISBN, 0L);

        assertNull(updated.getPhoto());
        verify(bookRepository).save(book);
        verify(photoRepository).deleteByPhotoFile("cover.png");
    }

    @Test
    void removePhotoWithWrongVersionIsRejectedBeforeAnythingIsDeleted() {
        when(bookRepository.findByIsbn(ISBN)).thenReturn(Optional.of(existingBook("cover.png")));

        assertThrows(ConflictException.class, () -> service.removeBookPhoto(ISBN, 9L));
        verify(photoRepository, never()).deleteByPhotoFile(anyString());
    }

    // suggestions

    private ReaderDetails readerWithInterests(List<Genre> interests) {
        final var reader = mock(ReaderDetails.class);
        when(reader.getInterestList()).thenReturn(interests);
        when(readerRepository.findByReaderNumber("2026/1")).thenReturn(Optional.of(reader));
        return reader;
    }

    @Test
    void suggestionsForAnUnknownReaderAreNotFound() {
        when(readerRepository.findByReaderNumber("2026/1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getBooksSuggestionsForReader("2026/1"));
    }

    @Test
    void suggestionsForAReaderWithoutInterestsAreNotFound() {
        readerWithInterests(List.of());

        assertThrows(NotFoundException.class, () -> service.getBooksSuggestionsForReader("2026/1"));
    }

    @Test
    void suggestionsAreLimitedPerGenreAndSkipGenresWithoutBooks() {
        final var romance = new Genre("Romance");
        final var poetry = new Genre("Poesia");
        readerWithInterests(List.of(fantasy, romance, poetry));
        final var b1 = existingBook(null);
        final var b2 = existingBook(null);
        final var b3 = existingBook(null);
        final var b4 = existingBook(null);
        when(bookRepository.findByGenre("Fantasia")).thenReturn(List.of(b1, b2, b3));
        when(bookRepository.findByGenre("Romance")).thenReturn(List.of());
        when(bookRepository.findByGenre("Poesia")).thenReturn(List.of(b4));

        final var suggestions = service.getBooksSuggestionsForReader("2026/1");

        assertEquals(List.of(b1, b2, b4), suggestions); // limit is 2 per genre
    }

    // search

    @Test
    void searchWithoutPageOrQueryUsesTheDefaults() {
        final var books = List.of(existingBook(null));
        when(bookRepository.searchBooks(any(), any())).thenReturn(books);

        assertSame(books, service.searchBooks(null, null));

        verify(bookRepository).searchBooks(new Page(1, 10), new SearchBooksQuery("", "", ""));
    }

    @Test
    void searchPassesTheGivenPageAndQuery() {
        final var page = new Page(2, 5);
        final var query = new SearchBooksQuery("Enc", "Fantasia", "João");

        service.searchBooks(page, query);

        verify(bookRepository).searchBooks(page, query);
    }

    @Test
    void saveDelegatesToTheRepository() {
        final var book = existingBook(null);

        assertSame(book, service.save(book));
        verify(bookRepository).save(book);
    }
}
