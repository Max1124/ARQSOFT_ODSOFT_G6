package pt.psoft.g1.psoftg1.system;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pt.psoft.g1.psoftg1.testutils.ApiIntegrationTestSupport;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Opaque-box system test (1.3.3.4.5): everything goes through HTTP with real JWT tokens obtained from the login
 * endpoint, so security, controllers, services, policies, persistence and mapping run together. The only direct
 * set-up is the librarian account (the application has no endpoint to create one) and the catalogue's genre and author.
 */
class LendingSystemTest extends ApiIntegrationTestSupport {
    private static final String LIBRARIAN_EMAIL = "maria@example.com";
    private static final String READER_EMAIL = "manuel@example.com";

    private final LocalDate today = LocalDate.now();
    private String librarianToken;
    private String readerToken;
    private String readerNumber;
    private final List<String> isbns = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        newLibrarian(LIBRARIAN_EMAIL);
        newGenre("Romance");
        final long authorNumber = newAuthor("Manuel Antonio Pina").getAuthorNumber();
        librarianToken = login(LIBRARIAN_EMAIL, PASSWORD);

        readerNumber = registerReader(READER_EMAIL);
        readerToken = login(READER_EMAIL, PASSWORD);

        for (int i = 0; i < 8; i++) {
            final String isbn = nextValidIsbn();
            isbns.add(isbn);
            mockMvc.perform(withToken(put("/api/books/" + isbn), librarianToken)
                            .param("title", "Livro " + i).param("description", "Descrição " + i)
                            .param("genre", "Romance").param("authors", String.valueOf(authorNumber)))
                    .andExpect(status().isCreated());
        }
    }

    // helpers: HTTP only

    private static MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }

    private String login(String username, String password) throws Exception {
        final MvcResult result = mockMvc.perform(post("/api/public/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andExpect(header().exists("Authorization")).andReturn();
        return result.getResponse().getHeader("Authorization");
    }

    private String registerReader(String email) throws Exception {
        final MvcResult result = mockMvc.perform(post("/api/readers")
                        .param("username", email).param("password", PASSWORD).param("fullName", "Manuel Sarapinto das Coives")
                        .param("birthDate", "2000-01-01").param("phoneNumber", "919191919").param("gdpr", "true"))
                .andExpect(status().isCreated()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.readerNumber");
    }

    private MvcResult lend(String isbn) throws Exception {
        return mockMvc.perform(withToken(post("/api/lendings"), librarianToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"isbn\":\"" + isbn + "\",\"readerNumber\":\"" + readerNumber + "\"}")).andReturn();
    }

    private String lendingNumberOf(MvcResult created) throws Exception {
        return JsonPath.read(created.getResponse().getContentAsString(), "$.lendingNumber");
    }

    private String currentEtag(String lendingNumber) throws Exception {
        return mockMvc.perform(withToken(get("/api/lendings/" + lendingNumber), readerToken))
                .andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");
    }

    private void giveBack(String lendingNumber) throws Exception {
        mockMvc.perform(withToken(patch("/api/lendings/" + lendingNumber), readerToken)
                        .header("If-Match", currentEtag(lendingNumber))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
    }

    // journeys

    @Test
    void readerRegistersBorrowsRenewsAndReturnsABook() throws Exception {
        final MvcResult created = lend(isbns.get(0));
        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        final String number = lendingNumberOf(created);

        mockMvc.perform(withToken(get("/api/lendings/" + number), readerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookTitle").value("Livro 0"))
                .andExpect(jsonPath("$.limitDate").value(today.plusDays(15).toString()))
                .andExpect(jsonPath("$.daysUntilReturn").value(15));

        mockMvc.perform(withToken(post("/api/lendings/" + number + "/renewal"), readerToken)
                        .header("If-Match", currentEtag(number)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limitDate").value(today.plusDays(22).toString()));

        giveBack(number);

        mockMvc.perform(withToken(get("/api/lendings/" + number), readerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnedDate").value(today.toString()))
                .andExpect(jsonPath("$.daysOverdue").doesNotExist());
    }

    @Test
    void standardLimitIsEnforcedWithAReadableReason() throws Exception {
        final List<String> numbers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            final MvcResult created = lend(isbns.get(i));
            assertThat(created.getResponse().getStatus()).isEqualTo(201);
            numbers.add(lendingNumberOf(created));
        }

        mockMvc.perform(withToken(post("/api/lendings"), librarianToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"" + isbns.get(3) + "\",\"readerNumber\":\"" + readerNumber + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("maximum number of active lendings")));

        // Giving one back frees a place: the policy is evaluated again on every request.
        giveBack(numbers.get(0));
        assertThat(lend(isbns.get(3)).getResponse().getStatus()).isEqualTo(201);
    }

    @Test
    void readerWhoReturnsOnTimeEarnsTheAdaptivePolicy() throws Exception {
        for (int i = 0; i < 5; i++) {
            giveBack(lendingNumberOf(lend(isbns.get(i))));
        }

        final MvcResult sixth = lend(isbns.get(5));

        assertThat(sixth.getResponse().getStatus()).isEqualTo(201);
        assertThat(sixth.getResponse().getContentAsString()).contains(today.plusDays(21).toString());
    }

    @Test
    void readerWithFewerCompletedLendingsStaysOnTheStandardPolicy() throws Exception {
        for (int i = 0; i < 4; i++) {
            giveBack(lendingNumberOf(lend(isbns.get(i))));
        }

        final MvcResult fifth = lend(isbns.get(4));

        assertThat(fifth.getResponse().getContentAsString()).contains(today.plusDays(15).toString());
    }

    // security

    @Test
    void requestsWithoutATokenAreRejected() throws Exception {
        mockMvc.perform(post("/api/lendings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"" + isbns.get(0) + "\",\"readerNumber\":\"" + readerNumber + "\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/books/" + isbns.get(0))).andExpect(status().isUnauthorized());
    }

    @Test
    void aTamperedTokenIsRejected() throws Exception {
        mockMvc.perform(withToken(get("/api/books/" + isbns.get(0)), librarianToken + "x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordDoesNotLogIn() throws Exception {
        mockMvc.perform(post("/api/public/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + READER_EMAIL + "\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Authorization"));
    }

    @Test
    void rolesLimitWhoCanLendAndWhoCanRenew() throws Exception {
        final String number = lendingNumberOf(lend(isbns.get(0)));

        mockMvc.perform(withToken(post("/api/lendings"), readerToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"" + isbns.get(1) + "\",\"readerNumber\":\"" + readerNumber + "\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(withToken(post("/api/lendings/" + number + "/renewal"), librarianToken)
                        .header("If-Match", currentEtag(number)))
                .andExpect(status().isForbidden());
        mockMvc.perform(withToken(put("/api/books/" + nextValidIsbn()), readerToken)
                        .param("title", "Proibido").param("genre", "Romance").param("authors", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anotherReaderCannotSeeOrChangeTheLending() throws Exception {
        final String number = lendingNumberOf(lend(isbns.get(0)));
        registerReader("joao@example.com");
        final String otherToken = login("joao@example.com", PASSWORD);

        mockMvc.perform(withToken(get("/api/lendings/" + number), otherToken)).andExpect(status().isForbidden());
        mockMvc.perform(withToken(post("/api/lendings/" + number + "/renewal"), otherToken)
                        .header("If-Match", currentEtag(number)))
                .andExpect(status().isForbidden());
    }
}
