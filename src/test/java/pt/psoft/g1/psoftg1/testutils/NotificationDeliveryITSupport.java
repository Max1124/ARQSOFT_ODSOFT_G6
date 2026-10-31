package pt.psoft.g1.psoftg1.testutils;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pt.psoft.g1.psoftg1.lendingmanagement.services.LendingService;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.services.CreateReaderRequest;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderService;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.awaitility.Awaitility.await;

/**
 * Shared set-up of the delivery integration tests (*IT). Unlike the other tests, these deliver through the real
 * external systems (Mailpit, the SMS Gateway for Android phone, Mockoon), so they are not part of {@code ./mvnw test}:
 * the default test selection only picks *Test classes. Run one explicitly, e.g. {@code ./mvnw test -Dtest=EmailNotificationDeliveryIT}.
 * <p>
 * Each test registers the reader through the application's own registration service (so reader number, ids and
 * version are generated as the application does), logs in with a real JWT, has a librarian lend a book through the
 * API and then runs the periodic check. The notice day is set to the lending period, so a lending made today
 * receives the due-date notice.
 */
@ExtendWith(OutputCaptureExtension.class)
public abstract class NotificationDeliveryITSupport extends ApiIntegrationTestSupport {
    /** Standard lending period; each *IT sets notification.due-date-notice-days to it. */
    protected static final int NOTICE_DAYS = 15;
    protected static final String READER_EMAIL = "1211225@isep.ipp.pt";
    protected static final String READER_PHONE = "924470976"; // stored without the country code, as the domain requires
    private static final String LIBRARIAN_EMAIL = "bibliotecaria.integracao@example.com";
    private static final String MOCKOON_LOGS = "http://localhost:8027/mockoon-admin/logs";

    @Autowired
    private ReaderService readerService;
    @Autowired
    protected LendingService lendingService;

    protected ReaderDetails reader;
    protected String readerToken;
    protected String lendingNumber;
    protected LocalDate limitDate;

    /** Fails with a clear message when a local service the test delivers to is not listening. */
    protected static void requireListening(String service, String host, int port, String hint) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000);
        } catch (IOException e) {
            fail(service + " is not listening on " + host + ":" + port + ". " + hint);
        }
    }

    @BeforeEach
    void registerReaderAndLendABook() throws Exception {
        final CreateReaderRequest request = new CreateReaderRequest();
        request.setUsername(READER_EMAIL);
        request.setPassword(PASSWORD);
        request.setFullName("Leitor Integracao");
        request.setBirthDate("2000-12-13");
        request.setPhoneNumber(READER_PHONE);
        request.setGdpr(true);
        request.setMarketing(true);
        request.setThirdParty(true);
        request.setInterestList(new ArrayList<>());
        reader = readerService.create(request, null);
        readerToken = login(READER_EMAIL);

        newLibrarian(LIBRARIAN_EMAIL);
        newGenre("Romance");
        final long authorNumber = newAuthor("Manuel Antonio Pina").getAuthorNumber();
        final String librarianToken = login(LIBRARIAN_EMAIL);
        final String isbn = nextValidIsbn();
        mockMvc.perform(withToken(put("/api/books/" + isbn), librarianToken)
                        .param("title", "O Inspetor Max").param("description", "Descrição")
                        .param("genre", "Romance").param("authors", String.valueOf(authorNumber)))
                .andExpect(status().isCreated());
        final MvcResult lent = mockMvc.perform(withToken(post("/api/lendings"), librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"" + isbn + "\",\"readerNumber\":\"" + reader.getReaderNumber() + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        lendingNumber = JsonPath.read(lent.getResponse().getContentAsString(), "$.lendingNumber");
        limitDate = LocalDate.parse(JsonPath.read(lent.getResponse().getContentAsString(), "$.limitDate"));
        assertThat(limitDate).as("limit date must be on the due-date notice day set by the *IT classes")
                .isEqualTo(LocalDate.now().plusDays(NOTICE_DAYS));
    }

    /** The reader the tests notify, with the data given for these tests. */
    protected void assertReaderIsAsSpecified() {
        assertThat(reader.getReader().getUsername()).isEqualTo(READER_EMAIL);
        assertThat(reader.getReaderNumber()).matches(LocalDate.now().getYear() + "/\\d+");
        assertThat(reader.getBirthDate().getBirthDate()).isEqualTo(LocalDate.of(2000, 12, 13));
        assertThat(reader.getPhoneNumber()).isEqualTo(READER_PHONE);
        assertThat(reader.isGdprConsent()).isTrue();
        assertThat(reader.isMarketingConsent()).isTrue();
        assertThat(reader.isThirdPartySharingConsent()).isTrue();
        assertThat(reader.getInterestList()).isEmpty();
        assertThat(reader.getVersion()).isNotNull();
    }

    /** The lending service logs a failing channel and continues, so a failure is only visible in the log. */
    protected static void assertChannelDidNotFail(CapturedOutput output, String channel) {
        assertThat(output.getAll()).doesNotContain("through channel '" + channel + "' failed");
    }

    protected static MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }

    protected String login(String username) throws Exception {
        final MvcResult result = mockMvc.perform(post("/api/public/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andExpect(header().exists("Authorization")).andReturn();
        return result.getResponse().getHeader("Authorization");
    }

    protected ResultActions choose(String channelsJson) throws Exception {
        return mockMvc.perform(withToken(put("/api/readers/notification-channels"), readerToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"channels\":" + channelsJson + "}"));
    }

    protected ResultActions choose(String channelsJson, String webhookUrl) throws Exception {
        return mockMvc.perform(withToken(put("/api/readers/notification-channels"), readerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"channels\":" + channelsJson + ",\"webhookUrl\":\"" + webhookUrl + "\"}"));
    }

    /** Forgets the requests Mockoon has recorded, so the next check only sees the ones of this test. */
    protected static void clearMockoonRequests() {
        mockoonLogs().post().uri("/purge").retrieve().toBodilessEntity();
    }

    /** No logged failure also holds when nothing was sent, so the request itself is read back from Mockoon. */
    protected static void assertMockoonAnsweredOneRequest(String path, String... expectedInBody) {
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            final String json = mockoonLogs().get().uri("?limit=100").retrieve().body(String.class);
            final List<Map<String, Object>> requests = JsonPath.read(json, "$[?(@.request.urlPath == '" + path + "')]");
            assertThat(requests).singleElement().satisfies(request -> {
                assertThat(JsonPath.<Integer>read(request, "$.response.statusCode")).isEqualTo(200);
                assertThat(JsonPath.<String>read(request, "$.request.body")).contains(expectedInBody);
            });
        });
    }

    private static RestClient mockoonLogs() {
        final String token = System.getenv("MOCKOON_ADMIN_TOKEN");
        if (token == null || token.isBlank()) {
            fail("Environment variable MOCKOON_ADMIN_TOKEN is not set: it must match Mockoon's --admin-api-token.");
        }
        return RestClient.builder().baseUrl(MOCKOON_LOGS).defaultHeader("Authorization", "Bearer " + token).build();
    }


}
