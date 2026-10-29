package pt.psoft.g1.psoftg1.readermanagement.api;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.services.LendingService;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.testutils.ApiIntegrationTestSupport;
import pt.psoft.g1.psoftg1.testutils.LocalNotificationReceiver;
import pt.psoft.g1.psoftg1.testutils.LocalSmtpReceiver;
import pt.psoft.g1.psoftg1.usermanagement.model.User;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Opaque-box collaboration SUT (TST-NOT-05): real controller, preference validation and storage, lending service
 * (periodic check and dispatch), channel composition, email and SMS channels, repositories and database.
 * Outside the SUT: test properties (webhook disabled), seeded lendings, a local SMTP receiver for email and a local
 * HTTP receiver for SMS. Authentication is simulated; the system test uses real tokens.
 */
@TestPropertySource(properties = {"notification.channels=email,sms", "notification.due-date-notice-days=3"})
class NotificationApiComponentTest extends ApiIntegrationTestSupport {
    private static final LocalSmtpReceiver SMTP = LocalSmtpReceiver.start();
    private static final LocalNotificationReceiver RECEIVER = LocalNotificationReceiver.start();

    @Autowired
    private LendingService lendingService;

    private final LocalDate today = LocalDate.now();

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
    void clearReceivers() {
        SMTP.clear();
        RECEIVER.clear();
    }

    private ResultActions choose(User user, String role, String channelsJson) throws Exception {
        return mockMvc.perform(put("/api/readers/notification-channels").with(asUser(user, role))
                .contentType(MediaType.APPLICATION_JSON).content("{\"channels\":" + channelsJson + "}"));
    }

    // preferences (CFG-NOT-02, QAS6)

    @Test
    void readerChoosesEnabledChannelsAndTheChoiceIsStored() throws Exception {
        final ReaderDetails reader = newReader("ana@example.com");

        choose(reader.getReader(), "READER", "[\"sms\",\"email\"]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channels[0]").value("email"))
                .andExpect(jsonPath("$.channels[1]").value("sms"));

        assertThat(readerRepository.findByUsername("ana@example.com").orElseThrow().getNotificationChannels())
                .containsExactlyInAnyOrder("email", "sms");
    }

    @Test
    void choiceWithADisabledChannelIsRejectedNamingItAndNothingIsStored() throws Exception {
        final ReaderDetails reader = newReader("ana@example.com");

        choose(reader.getReader(), "READER", "[\"email\",\"webhook\"]")
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Notification channel not enabled: webhook")));

        assertThat(readerRepository.findByUsername("ana@example.com").orElseThrow().getNotificationChannels()).isEmpty();
    }

    @Test
    void emptyChoiceIsRejected() throws Exception {
        final ReaderDetails reader = newReader("ana@example.com");

        choose(reader.getReader(), "READER", "[]")
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("At least one notification channel must be chosen")));
    }

    @Test
    void changingOneReadersChoiceLeavesAnotherReaderUnaffected() throws Exception {
        final ReaderDetails first = newReader("ana@example.com");
        final ReaderDetails second = newReader("rui@example.com");
        choose(first.getReader(), "READER", "[\"email\"]").andExpect(status().isOk());
        choose(second.getReader(), "READER", "[\"sms\"]").andExpect(status().isOk());

        choose(first.getReader(), "READER", "[\"email\",\"sms\"]").andExpect(status().isOk());

        assertThat(readerRepository.findByUsername("rui@example.com").orElseThrow().getNotificationChannels())
                .containsExactly("sms");
    }

    @Test
    void librarianCannotChooseChannels() throws Exception {
        choose(newLibrarian("maria@example.com"), "LIBRARIAN", "[\"email\"]").andExpect(status().isForbidden());
    }

    // periodic check and dispatch through the real channels (US6, US7, CFG-NOT-03)

    @Test
    void dueAndOverdueLendingsAreDeliveredOnlyThroughEachReadersEnabledChosenChannels() throws Exception {
        final Book book = newBook("O Inspetor Max", newGenre("Romance"), newAuthor("Manuel Antonio Pina"));
        final ReaderDetails ana = newReader("ana@example.com");
        final ReaderDetails rui = newReader("rui@example.com");
        choose(ana.getReader(), "READER", "[\"email\"]").andExpect(status().isOk());
        choose(rui.getReader(), "READER", "[\"sms\"]").andExpect(status().isOk());
        final Lending dueSoon = seedLending(book, ana, today.minusDays(12), null, 15);  // limit date in 3 days
        seedLending(book, ana, today.minusDays(5), null, 15);                            // limit date in 10 days
        final Lending overdue = seedLending(book, rui, today.minusDays(20), null, 15);   // 5 days overdue

        lendingService.checkDueAndOverdueLendings();

        assertThat(SMTP.received()).singleElement().satisfies(email -> {
            assertThat(email.recipients()).containsExactly("ana@example.com");
            assertThat(email.subject()).isEqualTo("Lending " + dueSoon.getLendingNumber() + " is due on " + today.plusDays(3));
            assertThat(email.text()).contains(dueSoon.getLendingNumber()).contains(today.plusDays(3).toString());
        });
        assertThat(RECEIVER.receivedAt("/sms")).singleElement().satisfies(sms -> assertThat(sms.body())
                .contains("\"phoneNumbers\":[\"+351919191919\"]").contains(overdue.getLendingNumber())
                .contains("5 day(s) overdue").contains("A fine of 10.00€ is being applied"));
    }
}
