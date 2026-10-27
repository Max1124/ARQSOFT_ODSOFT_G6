package pt.psoft.g1.psoftg1.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

/** Opaque-box system SUT: backend listening on a real HTTP port, real login/JWT and persistence.
 * Only initial librarian/catalogue fixtures bypass HTTP; no existing endpoint creates a genre.
 * Never invokes the API Ninjas lookup endpoint. Dedicated H2 database is discarded with the context. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:readerGenreSystem;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.profiles.active=test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReaderGenreSystemTest {
    @LocalServerPort int port;
    @Autowired UserRepository users;
    @Autowired GenreRepository genres;
    @Autowired AuthorRepository authors;
    @Autowired BookRepository books;
    @Autowired ObjectMapper json;
    private final HttpClient client = HttpClient.newHttpClient();
    private static final String PASSWORD = "Passw0rd!123";

    @Test void registerLoginReadAndRejectDuplicateThroughRealHttp() throws Exception {
        // Arrange: all reader setup uses public HTTP endpoints.
        String email = "reader.system@example.com";
        String form = "username=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(PASSWORD, StandardCharsets.UTF_8)
                + "&fullName=Maria+Silva&birthDate=2000-01-01&phoneNumber=912345678&gdpr=true";
        // Act
        var created = send("POST", "/api/readers", form, "application/x-www-form-urlencoded", null);
        // Assert
        assertThat(created.statusCode()).isEqualTo(201);
        JsonNode body = json.readTree(created.body());
        assertThat(body.path("email").asText()).isEqualTo(email);
        assertThat(body.path("readerNumber").asText()).matches("\\d{4}/\\d+");
        assertThat(created.headers().firstValue("ETag")).isPresent();

        // Arrange
        String token = login(email);
        // Act
        var read = send("GET", "/api/readers", null, null, token);
        // Assert
        assertThat(read.statusCode()).isEqualTo(200);
        assertThat(json.readTree(read.body()).path("phoneNumber").asText()).isEqualTo("912345678");
        assertThat(json.readTree(read.body()).path("readerNumber").asText())
                .isEqualTo(body.path("readerNumber").asText());

        // Act: the same identity cannot be registered twice.
        var duplicate = send("POST", "/api/readers", form, "application/x-www-form-urlencoded", null);
        // Assert
        assertThat(duplicate.statusCode()).isEqualTo(409);
        assertThat(duplicate.body()).contains("Username already exists");

        // Act: anonymous requests cannot read private reader data.
        var anonymous = send("GET", "/api/readers", null, null, null);
        // Assert
        assertThat(anonymous.statusCode()).isEqualTo(401);
    }

    @Test void genreRankingUsesCatalogueAndRequiresAuthentication() throws Exception {
        // Arrange: reference catalogue and librarian have no public creation endpoints here.
        users.save(Librarian.newLibrarian("genre.system.lib@example.com", PASSWORD, "Maria Silva"));
        var genre = genres.save(new Genre("SystemGenre"));
        var author = authors.save(new Author("Maria Silva", "A valid author biography.", null));
        books.save(new Book("9780132350884", "Clean Code", "Description", genre, List.of(author), null));
        String token = login("genre.system.lib@example.com");
        // Act
        var response = send("GET", "/api/genres/top5", null, null, token);
        // Assert
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("SystemGenre");
        // Act
        var anonymous = send("GET", "/api/genres/top5", null, null, null);
        // Assert
        assertThat(anonymous.statusCode()).isEqualTo(401);
    }

    private String login(String email) throws Exception {
        var response = send("POST", "/api/public/login",
                json.writeValueAsString(java.util.Map.of("username", email, "password", PASSWORD)),
                "application/json", null);
        assertThat(response.statusCode()).isEqualTo(200);
        return response.headers().firstValue("Authorization").orElseThrow();
    }

    private HttpResponse<String> send(String method, String path, String body, String type, String token)
            throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(java.time.Duration.ofSeconds(15));
        if (type != null) request.header("Content-Type", type);
        if (token != null) request.header("Authorization", "Bearer " + token);
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
