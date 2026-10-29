package pt.psoft.g1.psoftg1.system;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pt.psoft.g1.psoftg1.lendingmanagement.services.LendingService;
import pt.psoft.g1.psoftg1.testutils.ApiIntegrationTestSupport;
import pt.psoft.g1.psoftg1.testutils.LocalNotificationReceiver;
import pt.psoft.g1.psoftg1.testutils.LocalSmtpReceiver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Opaque-box system test (TST-NOT-05): readers, books, lendings and channel choices go through HTTP with real JWT
 * tokens, so security, controllers, services, composition, channels and persistence run together. All three channels
 * are enabled and deliver to local receivers: SMTP for email, HTTP for SMS and for the webhook URL the reader
 * registers (0 real messages). The scheduler is the only part not driven by HTTP: the test runs the periodic check
 * directly. The notice day is set to 15 so that a lending made today is on it.
 */
@TestPropertySource(properties = {"notification.channels=email,sms,webhook", "notification.due-date-notice-days=15"})
class NotificationSystemTest extends ApiIntegrationTestSupport {
    private static final LocalSmtpReceiver SMTP = LocalSmtpReceiver.start();
    private static final LocalNotificationReceiver RECEIVER = LocalNotificationReceiver.start();
    private static final String LIBRARIAN_EMAIL = "maria@example.com";
    private static final String READER_EMAIL = "manuel@example.com";

    @Autowired
    private LendingService lendingService;

    private String librarianToken;
    private String readerToken;
    private String readerNumber;
    private String lendingNumber;

    @DynamicPropertySource
    static void deliverToTheLocalReceivers(DynamicPropertyRegistry registry) {
        registry.add("notification.email.smtp.host", SMTP::host);
        registry.add("notification.email.smtp.port", SMTP::port);
        registry.add("notification.sms.gateway-url", () -> RECEIVER.url("/sms"));
    }

    @AfterAll
    static void stopReceivers() {
        SMTP.close();
        RECEIVER.close();
    }

    @BeforeEach
    void setUp() throws Exception {
        SMTP.clear();
        RECEIVER.clear();
        newLibrarian(LIBRARIAN_EMAIL);
        newGenre("Romance");
        final long authorNumber = newAuthor("Manuel Antonio Pina").getAuthorNumber();
        librarianToken = login(LIBRARIAN_EMAIL);

        final MvcResult registered = mockMvc.perform(post("/api/readers")
                        .param("username", READER_EMAIL).param("password", PASSWORD).param("fullName", "Manuel Sarapinto das Coives")
                        .param("birthDate", "2000-01-01").param("phoneNumber", "919191919").param("gdpr", "true"))
                .andExpect(status().isCreated()).andReturn();
        readerNumber = JsonPath.read(registered.getResponse().getContentAsString(), "$.readerNumber");
        readerToken = login(READER_EMAIL);

        final String isbn = nextValidIsbn();
        mockMvc.perform(withToken(put("/api/books/" + isbn), librarianToken)
                        .param("title", "O Inspetor Max").param("description", "Descrição")
                        .param("genre", "Romance").param("authors", String.valueOf(authorNumber)))
                .andExpect(status().isCreated());
        final MvcResult lent = mockMvc.perform(withToken(post("/api/lendings"), librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"" + isbn + "\",\"readerNumber\":\"" + readerNumber + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        lendingNumber = JsonPath.read(lent.getResponse().getContentAsString(), "$.lendingNumber");
    }

    // helpers: HTTP only

    private static MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }

    private String login(String username) throws Exception {
        final MvcResult result = mockMvc.perform(post("/api/public/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andExpect(header().exists("Authorization")).andReturn();
        return result.getResponse().getHeader("Authorization");
    }

    private ResultActions choose(String token, String channelsJson) throws Exception {
        return mockMvc.perform(withToken(put("/api/readers/notification-channels"), token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"channels\":" + channelsJson + "}"));
    }

    private ResultActions choose(String token, String channelsJson, String webhookUrl) throws Exception {
        return mockMvc.perform(withToken(put("/api/readers/notification-channels"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"channels\":" + channelsJson + ",\"webhookUrl\":\"" + webhookUrl + "\"}"));
    }

    // journeys

    @Test
    void readerChangesChannelsAndOnlyTheNewChannelsReceiveTheNextNotification() throws Exception {
        choose(readerToken, "[\"email\"]").andExpect(status().isOk());

        lendingService.checkDueAndOverdueLendings();

        assertThat(SMTP.received()).singleElement().satisfies(email -> {
            assertThat(email.recipients()).containsExactly(READER_EMAIL);
            assertThat(email.text()).contains(lendingNumber);
        });
        assertThat(RECEIVER.receivedAt("/sms")).isEmpty();
        assertThat(RECEIVER.receivedAt("/webhook")).isEmpty();

        // The new choice, with the reader's own webhook URL, applies to the next notification without restart (QAS6).
        SMTP.clear();
        choose(readerToken, "[\"sms\",\"webhook\"]", RECEIVER.url("/webhook")).andExpect(status().isOk());

        lendingService.checkDueAndOverdueLendings();

        assertThat(SMTP.received()).isEmpty();
        assertThat(RECEIVER.receivedAt("/sms")).singleElement()
                .satisfies(sms -> assertThat(sms.body()).contains("+351919191919").contains(lendingNumber));
        assertThat(RECEIVER.receivedAt("/webhook")).singleElement()
                .satisfies(hook -> assertThat(hook.body()).contains("\"event\":\"DUE_DATE_APPROACHING\"")
                        .contains("\"readerNumber\":\"" + readerNumber + "\"").contains(lendingNumber));
    }

    @Test
    void invalidChoicesAreRejectedThroughTheApi() throws Exception {
        choose(readerToken, "[\"email\",\"fax\"]")
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Notification channel not enabled: fax")));
        choose(readerToken, "[]").andExpect(status().isBadRequest());
        choose(readerToken, "[\"webhook\"]")
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("The webhook channel requires a webhook URL")));
        choose(readerToken, "[\"webhook\"]", "ftp://hooks.example.com")
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Webhook URL is not valid")));

        lendingService.checkDueAndOverdueLendings();

        assertThat(SMTP.received()).isEmpty(); // no choice was stored, so nothing is sent
        assertThat(RECEIVER.receivedAt("/sms")).isEmpty();
        assertThat(RECEIVER.receivedAt("/webhook")).isEmpty();
    }

    @Test
    void onlyAnAuthenticatedReaderCanChooseChannels() throws Exception {
        choose(librarianToken, "[\"email\"]").andExpect(status().isForbidden());
        mockMvc.perform(put("/api/readers/notification-channels").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channels\":[\"email\"]}"))
                .andExpect(status().isUnauthorized());
    }
}
