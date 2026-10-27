package pt.psoft.g1.psoftg1.genremanagement.api;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import pt.psoft.g1.psoftg1.testutils.ApiIntegrationTestSupport;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * SUT: genre HTTP component (real controller, service, generated mapper, JPA repository and H2).
 * This is a collaboration/integration test, not a unit test. Only authentication is supplied by
 * Spring Security test support. Fixtures use existing support; inherited transactions roll back.
 * A dedicated in-memory database prevents interference with other modules' component contexts.
 */
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:genre-component-tests;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.profiles.active=",
        "file.upload-dir=target/genre-component-uploads"
})
class GenreApiComponentTest extends ApiIntegrationTestSupport {
    private static org.springframework.test.web.servlet.request.RequestPostProcessor librarian() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"));
    }

    @Test
    void topFiveCountsBooksAndExcludesEmptyGenres() throws Exception {
        // Arrange: six distinct counts avoid relying on unspecified tie ordering.
        var author = newAuthor("Genre report author");
        newGenre("Empty");
        for (int count = 1; count <= 6; count++) {
            var genre = newGenre("Genre " + count);
            for (int book = 0; book < count; book++) {
                newBook("Book " + count + " " + book, genre, author);
            }
        }
        // Act
        var response = mockMvc.perform(get("/api/genres/top5").with(librarian()));
        // Assert
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(5))
                .andExpect(jsonPath("$.items[*].genreView.genre", contains("Genre 6", "Genre 5", "Genre 4", "Genre 3", "Genre 2")))
                .andExpect(jsonPath("$.items[*].bookCount", contains(6, 5, 4, 3, 2)));
    }

    @Test
    void averageUsesAllDaysOfLeapFebruaryAndIncludesBothMonthBoundaries() throws Exception {
        // Arrange: 29 in-range lendings yield exactly 1.0 per day; adjacent months must be excluded.
        var book = newBook("Leap month", newGenre("Fiction"), newAuthor("Leap author"));
        var reader = newReader("genre-leap@example.org");
        for (int day = 1; day <= 29; day++) {
            seedLending(book, reader, LocalDate.of(2024, 2, day), null, 15);
        }
        seedLending(book, reader, LocalDate.of(2024, 1, 31), null, 15);
        seedLending(book, reader, LocalDate.of(2024, 3, 1), null, 15);
        // Act
        var response = mockMvc.perform(post("/api/genres/avgLendingsPerGenre").with(librarian())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"page\":{\"number\":1,\"limit\":10},\"query\":{\"year\":2024,\"month\":2}}"));
        // Assert
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].genre").value("Fiction"))
                .andExpect(jsonPath("$.items[0].value").value(1.0));
    }

    @Test
    void monthlyCountsIncludeWindowBoundariesAndExcludeOutsideDates() throws Exception {
        // Arrange: capture one date for all relative fixtures.
        LocalDate today = LocalDate.now();
        LocalDate first = today.minusMonths(12);
        var book = newBook("Window", newGenre("History"), newAuthor("Window author"));
        var reader = newReader("genre-window@example.org");
        seedLending(book, reader, first, null, 15);
        seedLending(book, reader, today, null, 15);
        seedLending(book, reader, first.minusDays(1), null, 15);
        seedLending(book, reader, today.plusDays(1), null, 15);
        // Act
        var response = mockMvc.perform(get("/api/genres/lendingsPerMonthLastTwelveMonths").with(librarian()));
        // Assert: month buckets have no specified ordering, so select by year and month.
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[*].lendingsCount[*].genre", everyItem(is("History"))))
                .andExpect(jsonPath("$.items[*].lendingsCount[*].value", containsInAnyOrder(1, 1)))
                .andExpect(jsonPath("$.items[?(@.year == " + first.getYear() + " && @.month == " + first.getMonthValue() + ")].lendingsCount[0].value", contains(1)))
                .andExpect(jsonPath("$.items[?(@.year == " + today.getYear() + " && @.month == " + today.getMonthValue() + ")].lendingsCount[0].value", contains(1)));
    }

    @Test
    void durationAveragesOnlyReturnedLendingsWhoseStartIsInTheInclusiveInterval() throws Exception {
        // Arrange: durations 2 and 4 average to 3; return date may be outside the queried interval.
        var book = newBook("Duration", newGenre("Fiction"), newAuthor("Duration author"));
        var reader = newReader("genre-duration@example.org");
        seedLending(book, reader, LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 3), 15);
        seedLending(book, reader, LocalDate.of(2024, 2, 29), LocalDate.of(2024, 3, 4), 15);
        seedLending(book, reader, LocalDate.of(2024, 2, 15), null, 15);
        seedLending(book, reader, LocalDate.of(2024, 1, 31), LocalDate.of(2024, 2, 20), 30);
        seedLending(book, reader, LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 10), 15);
        // Act
        var response = mockMvc.perform(get("/api/genres/lendingsAverageDurationPerMonth").with(librarian())
                .param("startDate", "2024-02-01").param("endDate", "2024-02-29"));
        // Assert
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].year").value(2024))
                .andExpect(jsonPath("$.items[0].month").value(2))
                .andExpect(jsonPath("$.items[0].durationAverages.length()").value(1))
                .andExpect(jsonPath("$.items[0].durationAverages[0].genre").value("Fiction"))
                .andExpect(jsonPath("$.items[0].durationAverages[0].value").value(3.0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/top5", "/lendingsPerMonthLastTwelveMonths", "/lendingsAverageDurationPerMonth?startDate=2024-01-01&endDate=2024-12-31"})
    void emptyReportsReturnNotFound(String path) throws Exception {
        // Arrange: isolated database contains no report fixtures.
        // Act
        var response = mockMvc.perform(get("/api/genres" + path).with(librarian()));
        // Assert
        response.andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 13})
    void averageSearchRejectsInvalidMonthsThroughBeanValidation(int month) throws Exception {
        // Arrange
        String body = "{\"page\":{\"number\":1,\"limit\":10},\"query\":{\"year\":2024,\"month\":" + month + "}}";
        // Act
        var response = mockMvc.perform(post("/api/genres/avgLendingsPerGenre").with(librarian())
                .contentType(MediaType.APPLICATION_JSON).content(body));
        // Assert
        response.andExpect(status().isBadRequest());
    }

    @Test
    void readerCannotAccessGenreReports() throws Exception {
        // Arrange
        var authentication = jwt().authorities(new SimpleGrantedAuthority("ROLE_READER"));
        // Act
        var response = mockMvc.perform(get("/api/genres/top5").with(authentication));
        // Assert
        response.andExpect(status().isForbidden());
    }
}
