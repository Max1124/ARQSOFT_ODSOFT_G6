package pt.psoft.g1.psoftg1.bibliographicmanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Real controller/service/domain/repositories/transport/mappers; only remote HTTP endpoints are
 * local doubles. Includes a separate real HTTP + JWT system workflow. No live providers are
 * contacted.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.datasource.url=jdbc:h2:mem:bibliographic;DB_CLOSE_DELAY=-1",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.profiles.active=test"
        })
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BibliographicApiTest {
    static final String ISBN = "9780132350884";
    static final AtomicInteger googleCalls = new AtomicInteger(), openCalls = new AtomicInteger();
    static volatile int openStatus = 200;
    static final HttpServer providers = start();

    static HttpServer start() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext(
                    "/google",
                    ex -> {
                        googleCalls.incrementAndGet();
                        String body =
                                """
{"totalItems":1,"items":[{"volumeInfo":{"title":"Clean Code","industryIdentifiers":[{"identifier":"9780132350884"}],"authors":["Robert Martin"],"categories":["Computers"]}}]}
""";
                        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                        ex.sendResponseHeaders(200, bytes.length);
                        ex.getResponseBody().write(bytes);
                        ex.close();
                    });
            server.createContext(
                    "/open",
                    ex -> {
                        openCalls.incrementAndGet();
                        byte[] bytes = "{}".getBytes(StandardCharsets.UTF_8);
                        ex.sendResponseHeaders(openStatus, bytes.length);
                        ex.getResponseBody().write(bytes);
                        ex.close();
                    });
            server.start();
            return server;
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        String base = "http://127.0.0.1:" + providers.getAddress().getPort();
        r.add("bibliographic.sources", () -> "google-books,open-library");
        r.add("bibliographic.google-books.url", () -> base + "/google");
        r.add("bibliographic.open-library.url", () -> base + "/open");
    }

    @AfterAll
    static void close() {
        providers.stop(0);
    }

    @AfterEach
    void reset() {
        openStatus = 200;
    }

    @Autowired MockMvc mvc;
    @Autowired AuthorRepository authors;
    @Autowired GenreRepository genres;
    @Autowired BookRepository books;
    @Autowired UserRepository users;
    @Autowired ObjectMapper json;
    @LocalServerPort int port;
    final HttpClient client = HttpClient.newHttpClient();

    @Test
    void realCollaborationKeepsPartialSuccessAndDoesNotWriteCatalogue() throws Exception {
        // Arrange
        openStatus = 503;
        int before = googleCalls.get();
        boolean existed = books.findByIsbn(ISBN).isPresent();
        // Act
        var result =
                mvc.perform(
                        get("/api/bibliographic/" + ISBN)
                                .with(
                                        jwt().authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_LIBRARIAN"))));
        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.lookup.status").value("FOUND"))
                .andExpect(jsonPath("$.lookup.data.title").value("Clean Code"))
                .andExpect(
                        jsonPath("$.lookup.warnings[0]")
                                .value("Source open-library failed; lookup incomplete"));
        assertThat(googleCalls.get()).isEqualTo(before + 1);
        assertThat(books.findByIsbn(ISBN).isPresent()).isEqualTo(existed);
    }

    @Test
    void securityAndInvalidIsbnRejectBeforeNetwork() throws Exception {
        // Arrange
        int before = googleCalls.get();
        // Act / Assert
        mvc.perform(get("/api/bibliographic/" + ISBN)).andExpect(status().isUnauthorized());
        mvc.perform(
                        get("/api/bibliographic/" + ISBN)
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_READER"))))
                .andExpect(status().isForbidden());
        mvc.perform(
                        get("/api/bibliographic/invalid")
                                .with(
                                        jwt().authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_LIBRARIAN"))))
                .andExpect(status().isBadRequest());
        assertThat(googleCalls.get()).isEqualTo(before);
    }

    @Test
    void realHttpConfirmsAuthorThenRegistersWithoutManualBookFields() throws Exception {
        // Arrange: reference catalogue is explicitly created, never created by lookup.
        users.save(Librarian.newLibrarian("bib.lib@example.com", "Passw0rd!123", "Maria Silva"));
        var author =
                authors.save(new Author("Robert Martin", "An existing catalogue author.", null));
        genres.save(new Genre("Computers"));
        var login =
                send(
                        "POST",
                        "/api/public/login",
                        "{\"username\":\"bib.lib@example.com\",\"password\":\"Passw0rd!123\"}",
                        null);
        assertThat(login.statusCode()).isEqualTo(200);
        String token = login.headers().firstValue("Authorization").orElseThrow();
        // Act: matching name alone must not register the book.
        var incomplete = send("POST", "/api/bibliographic/" + ISBN + "/registration", "{}", token);
        // Assert
        assertThat(incomplete.statusCode()).isEqualTo(422);
        assertThat(books.findByIsbn(ISBN)).isEmpty();
        assertThat(json.readTree(incomplete.body()).path("needs").toString())
                .contains("Confirm catalogue author");
        // Act: explicitly confirm the source's author before trying ISBN-only registration again.
        var confirmation =
                send(
                        "PUT",
                        "/api/bibliographic/" + ISBN + "/author-confirmations",
                        json.writeValueAsString(
                                Map.of(
                                        "source",
                                        "google-books",
                                        "externalName",
                                        "Robert Martin",
                                        "authorId",
                                        author.getAuthorNumber())),
                        token);
        // Assert
        assertThat(confirmation.statusCode()).isEqualTo(204);
        // Act
        var registered = send("POST", "/api/bibliographic/" + ISBN + "/registration", "{}", token);
        // Assert: persisted result plus public response, not just non-null.
        assertThat(registered.statusCode()).isEqualTo(201);
        assertThat(json.readTree(registered.body()).path("book").path("title").asText())
                .isEqualTo("Clean Code");
        assertThat(books.findByIsbn(ISBN).orElseThrow().getTitle().toString())
                .isEqualTo("Clean Code");
        assertThat(authors.searchByNameName("Robert Martin")).hasSize(1);
        assertThat(
                        send("POST", "/api/bibliographic/" + ISBN + "/registration", "{}", token)
                                .statusCode())
                .isEqualTo(409);
    }

    HttpResponse<String> send(String method, String path, String body, String token)
            throws Exception {
        var request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .timeout(java.time.Duration.ofSeconds(15))
                        .header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        return client.send(
                request.method(method, HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
