package pt.psoft.g1.psoftg1.bookmanagement.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.testutils.ApiIntegrationTestSupport;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Opaque-box test of controller + service + repositories + database for the book module (1.3.3.4.4); only authentication is simulated. */
class BookApiComponentTest extends ApiIntegrationTestSupport {
    private Librarian librarian;
    private ReaderDetails reader;
    private Genre fantasy;
    private Genre romance;
    private Author author;

    @BeforeEach
    void setUp() {
        librarian = newLibrarian("maria@example.com");
        reader = newReader("manuel@example.com");
        fantasy = newGenre("Fantasia");
        romance = newGenre("Romance");
        author = newAuthor("Manuel Antonio Pina");
    }

    // A description is always sent: registering without one fails with a 500 because Book.getDescription() throws on null (as-is defect).
    private void register(String isbn, String title, Genre genre) throws Exception {
        mockMvc.perform(put("/api/books/" + isbn).with(asUser(librarian, Role.LIBRARIAN))
                        .param("title", title)
                        .param("description", "Descrição de " + title)
                        .param("genre", genre.toString())
                        .param("authors", String.valueOf(author.getAuthorNumber())))
                .andExpect(status().isCreated());
    }

    @Test
    void librarianRegistersABookThatCanBeReadBack() throws Exception {
        final String isbn = nextValidIsbn();

        mockMvc.perform(put("/api/books/" + isbn).with(asUser(librarian, Role.LIBRARIAN))
                        .param("title", "O Inspetor Max")
                        .param("description", "Um pastor-alemão detetive")
                        .param("genre", "Fantasia")
                        .param("authors", String.valueOf(author.getAuthorNumber())))
                .andExpect(status().isCreated())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.isbn").value(isbn))
                .andExpect(jsonPath("$.authors[0]").value("Manuel Antonio Pina"));

        mockMvc.perform(get("/api/books/" + isbn).with(asUser(reader.getReader(), Role.READER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("O Inspetor Max"))
                .andExpect(jsonPath("$.genre").value("Fantasia"))
                .andExpect(jsonPath("$.description").value("Um pastor-alemão detetive"));
        assertThat(bookRepository.findByIsbn(isbn)).isPresent();
    }

    @Test
    void registeringTheSameIsbnTwiceIsRejected() throws Exception {
        final String isbn = nextValidIsbn();
        register(isbn, "Primeiro", fantasy);

        mockMvc.perform(put("/api/books/" + isbn).with(asUser(librarian, Role.LIBRARIAN))
                        .param("title", "Segundo").param("genre", "Fantasia")
                        .param("authors", String.valueOf(author.getAuthorNumber())))
                .andExpect(status().isBadRequest());

        assertThat(bookRepository.findByTitle("Segundo")).isEmpty();
    }

    @Test
    void registeringWithAnUnknownGenreIsRejected() throws Exception {
        mockMvc.perform(put("/api/books/" + nextValidIsbn()).with(asUser(librarian, Role.LIBRARIAN))
                        .param("title", "Sem género").param("genre", "Inexistente")
                        .param("authors", String.valueOf(author.getAuthorNumber())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void readerCannotRegisterABook() throws Exception {
        mockMvc.perform(put("/api/books/" + nextValidIsbn()).with(asUser(reader.getReader(), Role.READER))
                        .param("title", "Proibido").param("genre", "Fantasia")
                        .param("authors", String.valueOf(author.getAuthorNumber())))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownIsbnIsNotFound() throws Exception {
        mockMvc.perform(get("/api/books/" + nextValidIsbn()).with(asUser(librarian, Role.LIBRARIAN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void librarianUpdatesTheBookWithTheCurrentVersion() throws Exception {
        final String isbn = nextValidIsbn();
        register(isbn, "Título antigo", fantasy);
        final String etag = mockMvc.perform(get("/api/books/" + isbn).with(asUser(librarian, Role.LIBRARIAN)))
                .andReturn().getResponse().getHeader("ETag");

        mockMvc.perform(patch("/api/books/" + isbn).with(asUser(librarian, Role.LIBRARIAN))
                        .header("If-Match", etag)
                        .param("title", "Título novo").param("genre", "Romance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Título novo"))
                .andExpect(jsonPath("$.genre").value("Romance"));
    }

    @Test
    void updateWithAStaleVersionIsRejectedAndChangesNothing() throws Exception {
        final String isbn = nextValidIsbn();
        register(isbn, "Título antigo", fantasy);

        mockMvc.perform(patch("/api/books/" + isbn).with(asUser(librarian, Role.LIBRARIAN))
                        .header("If-Match", "\"99\"")
                        .param("title", "Título novo"))
                .andExpect(status().isConflict());

        assertThat(bookRepository.findByIsbn(isbn).orElseThrow().getTitle().toString()).isEqualTo("Título antigo");
    }

    @Test
    void updateWithoutIfMatchIsRejected() throws Exception {
        final String isbn = nextValidIsbn();
        register(isbn, "Título antigo", fantasy);

        mockMvc.perform(patch("/api/books/" + isbn).with(asUser(librarian, Role.LIBRARIAN)).param("title", "Título novo"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findBooksJoinsTitleAndGenreCriteriaWithoutDuplicates() throws Exception {
        register(nextValidIsbn(), "A casa dos espíritos", fantasy);
        register(nextValidIsbn(), "Os Lusíadas", romance);

        mockMvc.perform(get("/api/books").with(asUser(reader.getReader(), Role.READER))
                        .param("title", "casa").param("genre", "Fantasia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("A casa dos espíritos"));
    }

    @Test
    void findBooksWithoutMatchesIsNotFound() throws Exception {
        mockMvc.perform(get("/api/books").with(asUser(reader.getReader(), Role.READER)).param("title", "zzzz"))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchCombinesPrefixesAndPaginates() throws Exception {
        register(nextValidIsbn(), "Alfa", fantasy);
        register(nextValidIsbn(), "Alvorada", fantasy);
        register(nextValidIsbn(), "Beta", romance);

        mockMvc.perform(post("/api/books/search").with(asUser(reader.getReader(), Role.READER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"page\":{\"number\":1,\"limit\":10},\"query\":{\"title\":\"Al\",\"genre\":\"Fant\",\"authorName\":\"\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[*].title", hasItem("Alvorada")));

        mockMvc.perform(post("/api/books/search").with(asUser(reader.getReader(), Role.READER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"page\":{\"number\":2,\"limit\":1},\"query\":{\"title\":\"\",\"genre\":\"\",\"authorName\":\"Manuel\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void suggestionsFollowTheInterestsOfTheReader() throws Exception {
        reader.setInterestList(new java.util.ArrayList<>(java.util.List.of(romance)));
        readerRepository.save(reader);
        register(nextValidIsbn(), "Os Lusíadas", romance);
        register(nextValidIsbn(), "A casa dos espíritos", fantasy);

        mockMvc.perform(get("/api/books/suggestions").with(asUser(reader.getReader(), Role.READER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Os Lusíadas"));
    }

    @Test
    void top5ListsTheMostLentBooksOfTheLastYear() throws Exception {
        final var popular = newBook("Muito lido", fantasy, author);
        final var other = newBook("Pouco lido", fantasy, author);
        seedLending(popular, reader, java.time.LocalDate.now().minusDays(10), java.time.LocalDate.now().minusDays(5), 15);
        seedLending(popular, reader, java.time.LocalDate.now().minusDays(20), java.time.LocalDate.now().minusDays(15), 15);
        seedLending(other, reader, java.time.LocalDate.now().minusDays(30), java.time.LocalDate.now().minusDays(25), 15);

        mockMvc.perform(get("/api/books/top5").with(asUser(librarian, Role.LIBRARIAN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].bookView.title").value("Muito lido"))
                .andExpect(jsonPath("$.items[0].lendingCount").value(2))
                .andExpect(jsonPath("$.items[1].bookView.title").value("Pouco lido"));
    }

    @Test
    void averageLendingDurationOfABookIsReported() throws Exception {
        final var book = newBook("Medido", fantasy, author);
        seedLending(book, reader, java.time.LocalDate.now().minusDays(30), java.time.LocalDate.now().minusDays(20), 15);
        seedLending(book, reader, java.time.LocalDate.now().minusDays(40), java.time.LocalDate.now().minusDays(20), 15);

        mockMvc.perform(get("/api/books/" + book.getIsbn() + "/avgDuration").with(asUser(librarian, Role.LIBRARIAN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageLendingDuration").value(15.0))
                .andExpect(jsonPath("$.book.isbn").value(book.getIsbn()));
    }
}
