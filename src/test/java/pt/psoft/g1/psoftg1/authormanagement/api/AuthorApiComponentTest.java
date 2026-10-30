package pt.psoft.g1.psoftg1.authormanagement.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.testutils.ApiIntegrationTestSupport;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Opaque-box test of controller + service + repositories + database for the author module (1.3.3.4.4); only authentication is simulated. */
class AuthorApiComponentTest extends ApiIntegrationTestSupport {
    private Librarian librarian;
    private ReaderDetails reader;
    private Genre fantasy;

    @BeforeEach
    void setUp() {
        librarian = newLibrarian("maria@example.com");
        reader = newReader("manuel@example.com");
        fantasy = newGenre("Fantasia");
    }

    private long register(String name) throws Exception {
        final String body = mockMvc.perform(post("/api/authors").with(asUser(librarian, Role.LIBRARIAN))
                        .param("name", name).param("bio", "Biografia de " + name + "."))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.authorNumber")).longValue();
    }

    private String etagOf(long authorNumber) throws Exception {
        return mockMvc.perform(get("/api/authors/" + authorNumber).with(asUser(librarian, Role.LIBRARIAN)))
                .andReturn().getResponse().getHeader("ETag");
    }

    // register

    @Test
    void librarianCanStoreAnInitialWithAPeriodWithoutChangingTheName() throws Exception {
        // Arrange / Act
        long id = register("Robert C. Martin");
        // Assert
        mockMvc.perform(get("/api/authors/" + id).with(asUser(librarian, Role.LIBRARIAN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Robert C. Martin"));
        assertThat(authorRepository.findByAuthorNumber(id).orElseThrow().getName())
                .isEqualTo("Robert C. Martin");
    }

    @Test
    void librarianRegistersAnAuthorThatCanBeReadBack() throws Exception {
        final String body = mockMvc.perform(post("/api/authors").with(asUser(librarian, Role.LIBRARIAN))
                        .param("name", "Manuel Antonio Pina")
                        .param("bio", "Jornalista e escritor português."))
                .andExpect(status().isCreated())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.name").value("Manuel Antonio Pina"))
                .andReturn().getResponse().getContentAsString();
        final long number = ((Number) JsonPath.read(body, "$.authorNumber")).longValue();

        mockMvc.perform(get("/api/authors/" + number).with(asUser(reader.getReader(), Role.READER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Jornalista e escritor português."))
                .andExpect(jsonPath("$._links.booksByAuthor", endsWith("/api/authors/" + number + "/books")));
        assertThat(authorRepository.findByAuthorNumber(number)).isPresent();
    }

    @Test
    void registeringWithAnInvalidNameIsRejectedAndNothingIsSaved() throws Exception {
        mockMvc.perform(post("/api/authors").with(asUser(librarian, Role.LIBRARIAN))
                        .param("name", "Manuel<script>").param("bio", "Bio."))
                .andExpect(status().isBadRequest());

        assertThat(authorRepository.searchByNameNameStartsWith("Manuel")).isEmpty();
    }

    @Test
    void readerCannotRegisterAnAuthor() throws Exception {
        mockMvc.perform(post("/api/authors").with(asUser(reader.getReader(), Role.READER))
                        .param("name", "Proibido").param("bio", "Bio."))
                .andExpect(status().isForbidden());

        assertThat(authorRepository.searchByNameName("Proibido")).isEmpty();
    }

    @Test
    void unknownAuthorIsNotFound() throws Exception {
        mockMvc.perform(get("/api/authors/999999").with(asUser(librarian, Role.LIBRARIAN)))
                .andExpect(status().isNotFound());
    }

    // update

    @Test
    void librarianUpdatesTheAuthorWithTheCurrentVersion() throws Exception {
        final long number = register("João Alberto");

        mockMvc.perform(patch("/api/authors/" + number).with(asUser(librarian, Role.LIBRARIAN))
                        .header("If-Match", etagOf(number))
                        .param("bio", "Nova biografia."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("João Alberto"))
                .andExpect(jsonPath("$.bio").value("Nova biografia."));

        assertThat(authorRepository.findByAuthorNumber(number).orElseThrow().getBio()).isEqualTo("Nova biografia.");
    }

    @Test
    void updateWithAStaleVersionIsRejectedAndChangesNothing() throws Exception {
        final long number = register("João Alberto");

        mockMvc.perform(patch("/api/authors/" + number).with(asUser(librarian, Role.LIBRARIAN))
                        .header("If-Match", "\"99\"")
                        .param("name", "Outro Nome"))
                .andExpect(status().isConflict());

        assertThat(authorRepository.findByAuthorNumber(number).orElseThrow().getName()).isEqualTo("João Alberto");
    }

    @Test
    void updateWithoutIfMatchIsRejected() throws Exception {
        final long number = register("João Alberto");

        mockMvc.perform(patch("/api/authors/" + number).with(asUser(librarian, Role.LIBRARIAN)).param("name", "Outro Nome"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateOfAnUnknownAuthorIsNotFound() throws Exception {
        mockMvc.perform(patch("/api/authors/999999").with(asUser(librarian, Role.LIBRARIAN))
                        .header("If-Match", "\"0\"").param("name", "Outro Nome"))
                .andExpect(status().isNotFound());
    }

    // search and relations

    @Test
    void searchByNameReturnsTheAuthorsWhoseNameStartsWithIt() throws Exception {
        register("João Alberto");
        register("Joana Vasconcelos");
        register("Maria João");

        mockMvc.perform(get("/api/authors").with(asUser(reader.getReader(), Role.READER)).param("name", "Jo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].name", containsInAnyOrder("João Alberto", "Joana Vasconcelos")));
    }

    @Test
    void booksOfAnAuthorAreListed() throws Exception {
        final Author pina = newAuthor("Manuel Antonio Pina");
        newBook("O Inspetor Max", fantasy, pina);
        newBook("Outro livro", fantasy, newAuthor("Outro Autor"));

        mockMvc.perform(get("/api/authors/" + pina.getAuthorNumber() + "/books").with(asUser(reader.getReader(), Role.READER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("O Inspetor Max"));
    }

    @Test
    void booksOfAnUnknownAuthorAreNotFound() throws Exception {
        mockMvc.perform(get("/api/authors/999999/books").with(asUser(reader.getReader(), Role.READER)))
                .andExpect(status().isNotFound());
    }

    @Test
    void coAuthorsAreListedWithTheirOwnBooks() throws Exception {
        final Author joao = newAuthor("João Alberto");
        final Author maria = newAuthor("Maria José");
        bookRepository.save(new Book(nextValidIsbn(), "Escrito a dois",
                "Descrição", fantasy, List.of(joao, maria), null));
        newBook("Só da Maria", fantasy, maria);

        mockMvc.perform(get("/api/authors/" + joao.getAuthorNumber() + "/coauthors").with(asUser(reader.getReader(), Role.READER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.author.name").value("João Alberto"))
                .andExpect(jsonPath("$.coauthors.length()").value(1))
                .andExpect(jsonPath("$.coauthors[0].name").value("Maria José"))
                .andExpect(jsonPath("$.coauthors[0].books[*].title", containsInAnyOrder("Escrito a dois", "Só da Maria")));
    }

    // top 5

    @Test
    void top5WithoutLendingsIsNotFound() throws Exception {
        mockMvc.perform(get("/api/authors/top5").with(asUser(reader.getReader(), Role.READER)))
                .andExpect(status().isNotFound());
    }

    @Test
    void top5ListsTheAuthorsWithMostLentBooksFirst() throws Exception {
        final var popular = newBook("Muito lido", fantasy, newAuthor("Autor Popular"));
        final var other = newBook("Pouco lido", fantasy, newAuthor("Autor Discreto"));
        final LocalDate today = LocalDate.now();
        seedLending(popular, reader, today.minusDays(10), today.minusDays(5), 15);
        seedLending(popular, reader, today.minusDays(20), today.minusDays(15), 15);
        seedLending(other, reader, today.minusDays(30), today.minusDays(25), 15);

        mockMvc.perform(get("/api/authors/top5").with(asUser(reader.getReader(), Role.READER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].authorName").value("Autor Popular"))
                .andExpect(jsonPath("$.items[0].lendingCount").value(2))
                .andExpect(jsonPath("$.items[1].authorName").value("Autor Discreto"));
    }

    // photo

    @Test
    void photoOfAnAuthorWithoutPhotoIsAnEmptyOk() throws Exception {
        final long number = register("João Alberto");

        mockMvc.perform(get("/api/authors/" + number + "/photo").with(asUser(reader.getReader(), Role.READER)))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[0]));
    }

    @Test
    void deletingTheMissingPhotoOfAnAuthorIsNotFound() throws Exception {
        final long number = register("João Alberto");

        mockMvc.perform(delete("/api/authors/" + number + "/photo").with(asUser(librarian, Role.LIBRARIAN)))
                .andExpect(status().isNotFound());
    }
}
