package pt.psoft.g1.psoftg1.bookmanagement.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.hibernate.StaleObjectStateException;
import org.springframework.test.util.ReflectionTestUtils;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.services.UpdateBookRequest;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BookTest {
    private final String validIsbn = "9782826012092";
    private final String validTitle = "Encantos de contar";
    private final Author validAuthor1 = new Author("João Alberto", "O João Alberto nasceu em Chaves e foi pedreiro a maior parte da sua vida.", null);
    private final Author validAuthor2 = new Author("Maria José", "A Maria José nasceu em Viseu e só come laranjas às segundas feiras.", null);
    private final Genre validGenre = new Genre("Fantasia");
    private ArrayList<Author> authors = new ArrayList<>();

    @BeforeEach
    void setUp(){
        authors.clear();
    }

    @Test
    void ensureIsbnNotNull(){
        authors.add(validAuthor1);
        assertThrows(IllegalArgumentException.class, () -> new Book(null, validTitle, null, validGenre, authors, null));
    }

    @Test
    void ensureTitleNotNull(){
        authors.add(validAuthor1);
        assertThrows(IllegalArgumentException.class, () -> new Book(validIsbn, null, null, validGenre, authors, null));
    }

    @Test
    void ensureGenreNotNull(){
        authors.add(validAuthor1);
        assertThrows(IllegalArgumentException.class, () -> new Book(validIsbn, validTitle, null,null, authors, null));
    }

    @Test
    void ensureAuthorsNotNull(){
        authors.add(validAuthor1);
        assertThrows(IllegalArgumentException.class, () -> new Book(validIsbn, validTitle, null, validGenre, null, null));
    }

    @Test
    void ensureAuthorsNotEmpty(){
        assertThrows(IllegalArgumentException.class, () -> new Book(validIsbn, validTitle, null, validGenre, authors, null));
    }

    @Test
    void ensureBookCreatedWithMultipleAuthors() {
        authors.add(validAuthor1);
        authors.add(validAuthor2);
        assertDoesNotThrow(() -> new Book(validIsbn, validTitle, null, validGenre, authors, null));
    }

    private Book bookWithVersion(String photoURI) {
        authors.add(validAuthor1);
        final var book = new Book(validIsbn, validTitle, "Uma descrição", validGenre, authors, photoURI);
        // The version is normally set by JPA on persist; tests set it to exercise the optimistic-lock guards.
        ReflectionTestUtils.setField(book, "version", 0L);
        return book;
    }

    @Test
    void ensureConstructorKeepsAllValues() {
        final var book = bookWithVersion("cover.png");

        assertEquals(validIsbn, book.getIsbn());
        assertEquals(validTitle, book.getTitle().toString());
        assertEquals("Uma descrição", book.getDescription());
        assertSame(validGenre, book.getGenre());
        assertEquals(List.of(validAuthor1), book.getAuthors());
        assertEquals("cover.png", book.getPhoto().getPhotoFile());
    }

    @Test
    void ensureBookWithoutPhotoUriHasNoPhoto() {
        assertNull(bookWithVersion(null).getPhoto());
    }

    @Test
    void ensurePatchChangesOnlyTheProvidedFields() {
        final var book = bookWithVersion(null);
        final var request = new UpdateBookRequest();
        request.setTitle("Novo título");

        book.applyPatch(0L, request);

        assertEquals("Novo título", book.getTitle().toString());
        assertEquals("Uma descrição", book.getDescription());
        assertSame(validGenre, book.getGenre());
        assertEquals(List.of(validAuthor1), book.getAuthors());
    }

    @Test
    void ensurePatchCanReplaceGenreAuthorsDescriptionAndPhoto() {
        final var book = bookWithVersion(null);
        final var newGenre = new Genre("Romance");
        final var request = new UpdateBookRequest();
        request.setGenreObj(newGenre);
        request.setAuthorObjList(List.of(validAuthor2));
        request.setDescription("Outra descrição");
        request.setPhotoURI("new.jpg");

        book.applyPatch(0L, request);

        assertSame(newGenre, book.getGenre());
        assertEquals(List.of(validAuthor2), book.getAuthors());
        assertEquals("Outra descrição", book.getDescription());
        assertEquals("new.jpg", book.getPhoto().getPhotoFile());
    }

    @Test
    void ensurePatchWithStaleVersionIsRejectedAndChangesNothing() {
        final var book = bookWithVersion(null);
        final var request = new UpdateBookRequest();
        request.setTitle("Novo título");

        assertThrows(StaleObjectStateException.class, () -> book.applyPatch(1L, request));
        assertEquals(validTitle, book.getTitle().toString());
    }

    @Test
    void ensurePatchValidatesTheNewTitle() {
        final var book = bookWithVersion(null);
        final var request = new UpdateBookRequest();
        request.setTitle("");

        assertThrows(IllegalArgumentException.class, () -> book.applyPatch(0L, request));
    }

    @Test
    void ensureRemovePhotoClearsThePhoto() {
        final var book = bookWithVersion("cover.png");

        book.removePhoto(0L);

        assertNull(book.getPhoto());
    }

    @Test
    void ensureRemovePhotoWithWrongVersionIsRejected() {
        final var book = bookWithVersion("cover.png");

        assertThrows(ConflictException.class, () -> book.removePhoto(5L));
        assertNotNull(book.getPhoto());
    }

}