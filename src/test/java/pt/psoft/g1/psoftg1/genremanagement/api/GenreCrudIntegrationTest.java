package pt.psoft.g1.psoftg1.genremanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

/** Real controller, service, domain and H2 persistence; authentication is controlled. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:genrecrud;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.profiles.active=test"
})
@AutoConfigureMockMvc
@Transactional
class GenreCrudIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired GenreRepository genres;
    @Autowired EntityManager em;

    private org.springframework.test.web.servlet.request.RequestPostProcessor librarian() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"));
    }

    @Test
    void createListRenameAndDeleteUnusedGenre() throws Exception {
        // Arrange
        String body = "{\"genre\":\"Computers\"}";
        // Act / Assert: exercise the complete catalogue workflow, including an unused genre.
        mvc.perform(post("/api/genres").with(librarian()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.genre").value("Computers"));
        mvc.perform(get("/api/genres").with(librarian()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[?(@.genre == 'Computers')]").isNotEmpty());
        mvc.perform(put("/api/genres/{name}", "Computers").with(librarian())
                .contentType(MediaType.APPLICATION_JSON).content("{\"genre\":\"Computing\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.genre").value("Computing"));
        assertThat(genres.findByString("Computers")).isEmpty();
        mvc.perform(delete("/api/genres/Computing").with(librarian())).andExpect(status().isNoContent());
        assertThat(genres.findByString("Computing")).isEmpty();
    }

    @Test
    void invalidDuplicateAndMissingRequestsDoNotChangeCatalogue() throws Exception {
        // Arrange
        genres.save(new Genre("Computers"));
        genres.save(new Genre("Science"));
        // Act / Assert
        for (String value : List.of("", "   ", "x".repeat(101))) {
            mvc.perform(post("/api/genres").with(librarian()).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"genre\":\"" + value + "\"}" )).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/genres").with(librarian()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"genre\":\" computers \"}")).andExpect(status().isConflict());
        mvc.perform(put("/api/genres/Science").with(librarian()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"genre\":\"Computers\"}")).andExpect(status().isConflict());
        mvc.perform(put("/api/genres/Missing").with(librarian()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"genre\":\"New\"}")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/genres/Missing").with(librarian())).andExpect(status().isNotFound());
        assertThat(genres.findByString("Science")).isPresent();
    }

    @Test
    void renamingPreservesBookAssociationAndDeletionIsRejected() throws Exception {
        // Arrange
        Genre genre = genres.save(new Genre("Computers"));
        Author author = new Author("Robert Martin", "Author", null);
        em.persist(author);
        Book book = new Book("9780132350884", "Clean Code", "Description", genre, List.of(author), null);
        em.persist(book);
        em.flush();
        // Act
        mvc.perform(put("/api/genres/Computers").with(librarian()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"genre\":\"Computing\"}")).andExpect(status().isOk());
        // Assert
        em.flush();
        em.refresh(book);
        assertThat(book.getGenre().getGenre()).isEqualTo("Computing");
        mvc.perform(delete("/api/genres/Computing").with(librarian())).andExpect(status().isConflict());
        assertThat(genres.findByString("Computing")).isPresent();
    }

    @Test
    void genreUsedOnlyByReaderCannotBeDeleted() throws Exception {
        // Arrange
        Genre genre = genres.save(new Genre("Interest"));
        Reader reader = Reader.newReader("genre-reader@example.com", "Passw0rd!123", "Test Reader");
        em.persist(reader);
        em.persist(new ReaderDetails(1, reader, "2000-01-01", "919191919", true, false, false, null, List.of(genre)));
        em.flush();
        // Act / Assert
        mvc.perform(delete("/api/genres/Interest").with(librarian())).andExpect(status().isConflict());
        assertThat(genres.findByString("Interest")).isPresent();
    }

    @Test
    void onlyLibrariansCanManageGenres() throws Exception {
        // Arrange / Act / Assert
        mvc.perform(get("/api/genres")).andExpect(status().isUnauthorized());
        var reader = jwt().authorities(new SimpleGrantedAuthority("ROLE_READER"));
        mvc.perform(get("/api/genres").with(reader)).andExpect(status().isForbidden());
        mvc.perform(post("/api/genres").with(reader).contentType(MediaType.APPLICATION_JSON)
                .content("{\"genre\":\"Computers\"}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/genres/Computers").with(reader).contentType(MediaType.APPLICATION_JSON)
                .content("{\"genre\":\"Computing\"}")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/genres/Computers").with(reader)).andExpect(status().isForbidden());
    }
}
