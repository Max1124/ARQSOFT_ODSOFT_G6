package pt.psoft.g1.psoftg1.system;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.services.CreateReaderRequest;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderService;
import pt.psoft.g1.psoftg1.testutils.ApiIntegrationTestSupport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration of the SMS channel over HTTP. In the pipelines, Mockoon stands in for SMS Gateway for Android, at
 * notification.sms.gateway-url in the test application.properties, and no SMS is sent. Not part of ./mvnw test; the
 * pipelines run it with jenkins/run-notification-its.sh.
 * For a demo with the real gateway, set its address and credentials in the test application.properties, keep Mockoon running, and run only dueDateNoticeIsAcceptedByThePhoneGatewayForTheReadersNumber; the SMS arriving is checked on the phone.
 * Not part of ./mvnw test; the pipelines run it with jenkins/run-notification-its.sh, which starts Mailpit.
 */
@ExtendWith(OutputCaptureExtension.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext
@TestPropertySource(properties = {"notification.channels=email", "notification.due-date-notice-days=15",
        "notification.check.cron=*/2 * * * * *", "spring.datasource.url=jdbc:h2:mem:scheduledcheck",
        "notification.email.smtp.host=localhost", "notification.email.smtp.port=1025",
        "notification.email.from=library@localhost"})
class ScheduledNotificationCheckIT extends ApiIntegrationTestSupport {
    private static final String MAILPIT_MESSAGES = "http://localhost:8025/api/v1/messages";
    private static final String READER_EMAIL = "1211225@isep.ipp.pt";
    private static final String LIBRARIAN_EMAIL = "bibliotecaria.agendamento@example.com";

    @Autowired
    private ReaderService readerService;

    @BeforeAll
    static void mailpitIsRunning() {
        requireListening("Mailpit SMTP", 1025);
        requireListening("Mailpit API", 8025);
    }

    @Test
    void scheduleFiresTheCheckAndTheDueDateNoticeReachesMailpit(CapturedOutput output) throws Exception {
        final Instant sentAfter = Instant.now().minusSeconds(1);
        final ReaderDetails reader = registerReader();
        final String readerToken = login(READER_EMAIL);
        final MvcResult lent = lendABookTo(reader);
        final String lendingNumber = JsonPath.read(lent.getResponse().getContentAsString(), "$.lendingNumber");
        final String limitDate = JsonPath.read(lent.getResponse().getContentAsString(), "$.limitDate");

        mockMvc.perform(withToken(put("/api/readers/notification-channels"), readerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"channels\":[\"email\"]}"))
                .andExpect(status().isOk());

        final String expectedSubject = "Lending " + lendingNumber + " is due on " + limitDate;
        await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(messagesInMailpitSince(sentAfter))
                        .anyMatch(message -> expectedSubject.equals(message.get("Subject"))));
        assertThat(output.getAll()).doesNotContain("through channel 'email' failed");
    }

    private ReaderDetails registerReader() {
        final CreateReaderRequest request = new CreateReaderRequest();
        request.setUsername(READER_EMAIL);
        request.setPassword(PASSWORD);
        request.setFullName("Leitor Agendamento");
        request.setBirthDate("2000-12-13");
        request.setPhoneNumber("924470976");
        request.setGdpr(true);
        request.setMarketing(true);
        request.setThirdParty(true);
        request.setInterestList(new ArrayList<>());
        return readerService.create(request, null);
    }

    private MvcResult lendABookTo(ReaderDetails reader) throws Exception {
        newLibrarian(LIBRARIAN_EMAIL);
        newGenre("Romance");
        final long authorNumber = newAuthor("Manuel Antonio Pina").getAuthorNumber();
        final String librarianToken = login(LIBRARIAN_EMAIL);
        final String isbn = nextValidIsbn();
        mockMvc.perform(withToken(put("/api/books/" + isbn), librarianToken)
                        .param("title", "O Inspetor Max").param("description", "Descrição")
                        .param("genre", "Romance").param("authors", String.valueOf(authorNumber)))
                .andExpect(status().isCreated());
        return mockMvc.perform(withToken(post("/api/lendings"), librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"" + isbn + "\",\"readerNumber\":\"" + reader.getReaderNumber() + "\"}"))
                .andExpect(status().isCreated()).andReturn();
    }

    private String login(String username) throws Exception {
        return mockMvc.perform(post("/api/public/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getHeader("Authorization");
    }

    private static MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }

    private static void requireListening(String service, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 2000);
        } catch (IOException e) {
            fail(service + " is not listening on localhost:" + port + ". Start Mailpit before running this test.");
        }
    }

    // Mailpit lists the newest messages first; only the ones received during this test are kept.
    private static List<Map<String, Object>> messagesInMailpitSince(Instant since) {
        final String json = RestClient.create().get().uri(MAILPIT_MESSAGES).retrieve().body(String.class);
        final List<Map<String, Object>> messages = JsonPath.read(json, "$.messages");
        return messages.stream()
                .filter(message -> !OffsetDateTime.parse((String) message.get("Created")).toInstant().isBefore(since))
                .toList();
    }
}
