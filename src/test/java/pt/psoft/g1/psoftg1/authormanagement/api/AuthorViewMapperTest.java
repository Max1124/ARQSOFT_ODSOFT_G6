package pt.psoft.g1.psoftg1.authormanagement.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Transparent-box test of the author view mapping and its HATEOAS links. */
class AuthorViewMapperTest {
    private AuthorViewMapper mapper;
    private Author joao;
    private Author maria;
    private Book encantos;

    @BeforeEach
    void setUp() {
        // Links are built from the current request, so a request context must exist.
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        mapper = new AuthorViewMapperImpl();

        joao = new Author("João Alberto", "O João Alberto nasceu em Chaves e foi pedreiro.", null);
        maria = new Author("Maria José", "A Maria José nasceu em Viseu e come laranjas.", "maria.png");
        ReflectionTestUtils.setField(joao, "authorNumber", 1L);
        ReflectionTestUtils.setField(maria, "authorNumber", 2L);
        encantos = new Book("9782826012092", "Encantos de contar", "Uma descrição", new Genre("Fantasia"), List.of(joao, maria), null);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void viewHasTheAuthorFields() {
        final var view = mapper.toAuthorView(joao);

        assertEquals(1L, view.getAuthorNumber());
        assertEquals("João Alberto", view.getName());
        assertEquals("O João Alberto nasceu em Chaves e foi pedreiro.", view.getBio());
        assertNull(view.getPhoto());
    }

    @Test
    void viewHasLinksToTheAuthorItsBooksAndItsPhoto() {
        final Map<String, Object> links = mapper.toAuthorView(maria).get_links();

        assertEquals("http://localhost/api/authors/2", links.get("author"));
        assertEquals("http://localhost/api/authors/2/books", links.get("booksByAuthor"));
        assertEquals("http://localhost/api/authors/2/photo", links.get("photo"));
    }

    @Test
    void listMappingKeepsTheOrderOfTheAuthors() {
        final var views = mapper.toAuthorView(List.of(maria, joao));

        assertEquals(List.of("Maria José", "João Alberto"), views.stream().map(AuthorView::getName).toList());
    }

    @Test
    void coAuthorViewHasTheNameTheLinksAndTheShortViewOfEachBook() {
        final var view = mapper.toCoAuthorView(maria, List.of(encantos));

        assertEquals("Maria José", view.getName());
        assertEquals("http://localhost/api/authors/2", view.get_links().get("author"));
        assertEquals(1, view.getBooks().size());
        assertEquals("Encantos de contar", view.getBooks().get(0).getTitle());
        assertEquals("9782826012092", view.getBooks().get(0).getIsbn());
        assertEquals("http://localhost/api/books/9782826012092", view.getBooks().get(0).get_links());
    }

    @Test
    void coAuthorBooksViewWrapsTheAuthorAndACopyOfTheCoAuthors() {
        final var coAuthors = new ArrayList<>(List.of(mapper.toCoAuthorView(maria, List.of(encantos))));

        final var view = mapper.toAuthorCoAuthorBooksView(joao, coAuthors);

        assertEquals("João Alberto", view.getAuthor().getName());
        assertEquals(coAuthors, view.getCoauthors());
        assertNotSame(coAuthors, view.getCoauthors());
    }
}
