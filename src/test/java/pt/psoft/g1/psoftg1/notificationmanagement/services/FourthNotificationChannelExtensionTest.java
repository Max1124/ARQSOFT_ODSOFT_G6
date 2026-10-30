package pt.psoft.g1.psoftg1.notificationmanagement.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.FineRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.services.LendingServiceImpl;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.LendingPolicySelector;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.ReaderLendingHistoryProvider;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.readermanagement.services.NotificationPreferenceService;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Extension experiment of QAS2 (Extensibility, Analysis — Iteration 2): a fourth channel is added next to the three
 * installed ones without changing any production class. Collaboration SUT: real composition, preference validation,
 * periodic check and dispatch in LendingServiceImpl, and the real email, SMS and webhook channels.
 * Outside the SUT: repositories, the mail sender and the HTTP mock servers, so no message is sent (CON-TST-8).
 */
class FourthNotificationChannelExtensionTest {
    private static final String PUSH = "push";
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);
    private static final int NOTICE_DAYS = 3;
    private static final String USERNAME = "reader1@example.com";
    private static Book book;

    /** The new mechanism: implements only the contract, and is registered like any installed channel. */
    static final class PushNotificationService implements NotificationService {
        private final List<LendingNotification> sent = new ArrayList<>();

        @Override
        public String channel() {
            return PUSH;
        }

        @Override
        public void send(String readerNumber, LendingNotification notification) {
            sent.add(notification);
        }
    }

    private final ReaderRepository readerRepository = mock(ReaderRepository.class);
    private final LendingRepository lendingRepository = mock(LendingRepository.class);
    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final RestClient.Builder smsHttp = RestClient.builder();
    private final RestClient.Builder webhookHttp = RestClient.builder();
    // No expectations: any request to these servers fails the test.
    private final MockRestServiceServer smsGateway = MockRestServiceServer.bindTo(smsHttp).build();
    private final MockRestServiceServer webhookReceiver = MockRestServiceServer.bindTo(webhookHttp).build();
    private final PushNotificationService push = new PushNotificationService();
    private final List<String> instantiated = new ArrayList<>();

    private ReaderDetails reader;
    private NotificationChannelComposition composition;

    @BeforeAll
    static void fixtures() {
        book = new Book("9782826012092", "O Inspetor Max", "descricao",
                new Genre("Romance"), List.of(new Author("Manuel Antonio Pina", "bio", null)), null);
    }

    @BeforeEach
    void installTheThreeChannelsAndTheNewOne() {
        reader = new ReaderDetails(1, Reader.newReader(USERNAME, "Manuelino123!", "Manuel Sarapinto das Coives"),
                "2000-01-01", "919191919", true, true, true, null, null);
        when(readerRepository.findByUsername(USERNAME)).thenReturn(Optional.of(reader));
        when(readerRepository.findByReaderNumber(reader.getReaderNumber())).thenReturn(Optional.of(reader));
        when(readerRepository.save(any(ReaderDetails.class))).thenAnswer(call -> call.getArgument(0));

        final Map<String, Supplier<NotificationService>> installed = new LinkedHashMap<>();
        installed.put(EmailNotificationService.CHANNEL, tracked(EmailNotificationService.CHANNEL,
                () -> new EmailNotificationService(readerRepository, mailSender, "library@example.com")));
        installed.put(SMSNotificationService.CHANNEL, tracked(SMSNotificationService.CHANNEL,
                () -> new SMSNotificationService(readerRepository, smsHttp, "http://sms-gateway.test/message", "u", "p", "+351")));
        installed.put(WebhookNotificationService.CHANNEL, tracked(WebhookNotificationService.CHANNEL,
                () -> new WebhookNotificationService(readerRepository, webhookHttp)));
        installed.put(PUSH, tracked(PUSH, () -> push)); // the only addition: one registration entry
        composition = new NotificationChannelComposition(installed);
    }

    @AfterEach
    void noExistingHttpChannelWasCalled() {
        smsGateway.verify();
        webhookReceiver.verify();
    }

    private Supplier<NotificationService> tracked(String id, Supplier<NotificationService> channel) {
        return () -> {
            instantiated.add(id);
            return channel.get();
        };
    }

    private LendingServiceImpl lendingServiceWith(List<NotificationService> enabled) {
        final Clock clock = Clock.fixed(TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        return new LendingServiceImpl(lendingRepository, mock(FineRepository.class), mock(BookRepository.class),
                readerRepository, clock, mock(ReaderLendingHistoryProvider.class), mock(LendingPolicySelector.class),
                200, enabled, new NotificationParameters(NOTICE_DAYS));
    }

    /** An outstanding 15-day lending whose limit date is {@code days} after the fixed today (negative: overdue). */
    private Lending outstandingDueIn(int days) {
        final Lending lending = new Lending(book, reader, 1, 15, 200, TODAY.plusDays(days - 15L));
        when(lendingRepository.listOutstanding()).thenReturn(List.of(lending));
        return lending;
    }

    @Test
    void enabledAndChosenFourthChannelReceivesTheDueDateNoticeAndTheOthersDoNot() {
        final List<NotificationService> enabled = composition.enabledChannels("email,sms,webhook,push");
        new NotificationPreferenceService(readerRepository, enabled).validateAndStore(USERNAME, List.of(PUSH), null);
        final Lending lending = outstandingDueIn(NOTICE_DAYS);

        lendingServiceWith(enabled).checkDueAndOverdueLendings();

        assertThat(push.sent).containsExactly(LendingNotification.dueDateApproaching(
                lending.getLendingNumber(), "O Inspetor Max", lending.getLimitDate()));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void fourthChannelIsNotifiedTogetherWithAnExistingChannel() {
        final List<NotificationService> enabled = composition.enabledChannels("email,push");
        new NotificationPreferenceService(readerRepository, enabled).validateAndStore(USERNAME, List.of("email", PUSH), null);
        final Lending lending = outstandingDueIn(-5);

        lendingServiceWith(enabled).checkDueAndOverdueLendings();

        assertThat(push.sent).containsExactly(LendingNotification.overdue(
                lending.getLendingNumber(), "O Inspetor Max", lending.getLimitDate(), 5, 1000));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void fourthChannelNotListedAtSetupIsNotCreatedAndCannotBeChosen() {
        final List<NotificationService> enabled = composition.enabledChannels("email");

        assertThat(instantiated).containsExactly("email");
        assertThatThrownBy(() -> new NotificationPreferenceService(readerRepository, enabled)
                .validateAndStore(USERNAME, List.of(PUSH), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Notification channel not enabled: push");
        assertThat(reader.getNotificationChannels()).isEmpty();
    }
}
