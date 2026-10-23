package pt.psoft.g1.psoftg1.bookmanagement.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookCountDTO;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Transparent-box test of the book view mapping and its HATEOAS links. */
class BookViewMapperTest {
    private static final String ISBN = "9782826012092";

    private BookViewMapper mapper;
    private Book book;

    @BeforeEach
    void setUp() {
        // Links are built from the current request, so a request context must exist.
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        mapper = new BookViewMapperImpl();

        final var author1 = new Author("João Alberto", "O João Alberto nasceu em Chaves e foi pedreiro.", null);
        final var author2 = new Author("Maria José", "A Maria José nasceu em Viseu e come laranjas.", null);
        ReflectionTestUtils.setField(author1, "authorNumber", 1L);
        ReflectionTestUtils.setField(author2, "authorNumber", 2L);
        book = new Book(ISBN, "Encantos de contar", "Uma descrição", new Genre("Fantasia"), List.of(author1, author2), null);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void viewHasTheBookFieldsAndTheAuthorNames() {
        final var view = mapper.toBookView(book);

        assertEquals(ISBN, view.getIsbn());
        assertEquals("Encantos de contar", view.getTitle());
        assertEquals("Fantasia", view.getGenre());
        assertEquals("Uma descrição", view.getDescription());
        assertEquals(List.of("João Alberto", "Maria José"), view.getAuthors());
    }

    @Test
    @SuppressWarnings("unchecked")
    void viewHasLinksToTheBookItsAuthorsAndItsPhoto() {
        final Map<String, Object> links = mapper.toBookView(book).get_links();

        assertEquals("http://localhost/api/books/" + ISBN, links.get("self"));
        assertEquals("http://localhost/api/books/" + ISBN + "/photo", links.get("photo"));
        assertEquals(List.of(Map.of("href", "http://localhost/api/authors/1"), Map.of("href", "http://localhost/api/authors/2")),
                links.get("authors"));
    }

    @Test
    void listMappingKeepsTheOrderOfTheBooks() {
        final var views = mapper.toBookView(List.of(book, book));

        assertEquals(2, views.size());
        assertEquals(ISBN, views.get(0).getIsbn());
    }

    @Test
    void countViewWrapsTheBookViewAndTheLendingCount() {
        final var view = mapper.toBookCountView(new BookCountDTO(book, 4));

        assertEquals(ISBN, view.getBookView().getIsbn());
        assertEquals(4L, view.getLendingCount());
    }

    @Test
    void averageViewWrapsTheBookViewAndTheAverage() {
        final var view = mapper.toBookAverageLendingDurationView(book, 12.5);

        assertEquals(ISBN, view.getBook().getIsbn());
        assertEquals(12.5, view.getAverageLendingDuration());
    }
}
