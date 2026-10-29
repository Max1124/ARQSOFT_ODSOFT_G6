package pt.psoft.g1.psoftg1.lendingmanagement.services;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.FineRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.LendingPolicySelector;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.ReaderLendingHistoryProvider;
import pt.psoft.g1.psoftg1.notificationmanagement.services.LendingNotification;
import pt.psoft.g1.psoftg1.notificationmanagement.services.NotificationParameters;
import pt.psoft.g1.psoftg1.notificationmanagement.services.NotificationService;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Class SUT: the periodic check and dispatch in LendingServiceImpl (TST-NOT-01, TST-NOT-04).
 * Each channel is a recording double, repositories are doubles and "today" is a fixed clock, so no message is sent.
 * Lendings and readers are prepared fixtures built with fixed dates.
 */
class LendingServiceImplNotificationTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);
    private static final int NOTICE_DAYS = 3;
    private static Book book;

    private final LendingRepository lendingRepository = mock(LendingRepository.class);
    private final ReaderRepository readerRepository = mock(ReaderRepository.class);
    private final Clock clock = Clock.fixed(TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    private final RecordingChannel email = new RecordingChannel("email");
    private final RecordingChannel sms = new RecordingChannel("sms");
    private final RecordingChannel webhook = new RecordingChannel("webhook");
    private int nextSeq = 1;

    /** Records reader, channel and content of every call (QAS10); can be told to fail after recording. */
    static final class RecordingChannel implements NotificationService {
        record Call(String readerNumber, LendingNotification notification) {}

        private final String id;
        private final List<Call> calls = new ArrayList<>();
        private RuntimeException failure;

        RecordingChannel(String id) {
            this.id = id;
        }

        @Override
        public String channel() {
            return id;
        }

        @Override
        public void send(String readerNumber, LendingNotification notification) {
            calls.add(new Call(readerNumber, notification));
            if (failure != null) {
                throw failure;
            }
        }
    }

    @BeforeAll
    static void fixtures() {
        book = new Book("9782826012092", "O Inspetor Max", "descricao",
                new Genre("Romance"), List.of(new Author("Manuel Antonio Pina", "bio", null)), null);
    }

    private LendingServiceImpl serviceWithEnabled(NotificationService... enabled) {
        return new LendingServiceImpl(lendingRepository, mock(FineRepository.class), mock(BookRepository.class),
                readerRepository, clock, mock(ReaderLendingHistoryProvider.class), mock(LendingPolicySelector.class),
                200, List.of(enabled), new NotificationParameters(NOTICE_DAYS));
    }

    private ReaderDetails readerChoosing(int number, String... channels) {
        final ReaderDetails reader = new ReaderDetails(number,
                Reader.newReader("reader" + number + "@example.com", "Manuelino123!", "Manuel Sarapinto das Coives"),
                "2000-01-01", "919191919", true, true, true, null, null);
        if (channels.length > 0) {
            reader.chooseNotificationChannels(Set.copyOf(Arrays.asList(channels)));
        }
        when(readerRepository.findByReaderNumber(reader.getReaderNumber())).thenReturn(Optional.of(reader));
        return reader;
    }

    /** An outstanding 15-day lending whose limit date is {@code days} after the fixed today (negative: overdue). */
    private Lending dueIn(ReaderDetails reader, int days) {
        return new Lending(book, reader, nextSeq++, 15, 200, TODAY.plusDays(days - 15L));
    }

    private void outstanding(Lending... lendings) {
        when(lendingRepository.listOutstanding()).thenReturn(List.of(lendings));
    }

    private static LendingNotification dueNotice(Lending lending) {
        return LendingNotification.dueDateApproaching(lending.getLendingNumber(), "O Inspetor Max", lending.getLimitDate());
    }

    // dispatch: intersection of enabled and chosen channels (CFG-NOT-03)

    @Test
    void readerIsNotifiedThroughEachChosenEnabledChannelAndThroughNoOther() {
        final ReaderDetails reader = readerChoosing(1, "email", "sms");
        final Lending lending = dueIn(reader, NOTICE_DAYS);
        outstanding(lending);

        serviceWithEnabled(email, sms, webhook).checkDueAndOverdueLendings();

        final RecordingChannel.Call expected = new RecordingChannel.Call(reader.getReaderNumber(), dueNotice(lending));
        assertThat(email.calls).containsExactly(expected);
        assertThat(sms.calls).containsExactly(expected);
        assertThat(webhook.calls).isEmpty();
    }

    @Test
    void chosenChannelThatIsNotEnabledIsNeverCalled() {
        final ReaderDetails reader = readerChoosing(1, "sms");
        outstanding(dueIn(reader, NOTICE_DAYS));

        serviceWithEnabled(email).checkDueAndOverdueLendings(); // sms is not enabled: empty intersection

        assertThat(email.calls).isEmpty();
        assertThat(sms.calls).isEmpty();
    }

    @Test
    void readerWhoChoseEveryEnabledChannelIsNotifiedOnceThroughEach() {
        final ReaderDetails reader = readerChoosing(1, "email", "sms", "webhook");
        outstanding(dueIn(reader, NOTICE_DAYS));

        serviceWithEnabled(email, sms, webhook).checkDueAndOverdueLendings();

        assertThat(email.calls).hasSize(1);
        assertThat(sms.calls).hasSize(1);
        assertThat(webhook.calls).hasSize(1);
    }

    @Test
    void readerWhoHasNotChosenAnyChannelReceivesNothing() {
        outstanding(dueIn(readerChoosing(1), NOTICE_DAYS));

        serviceWithEnabled(email, sms, webhook).checkDueAndOverdueLendings();

        assertThat(email.calls).isEmpty();
        assertThat(sms.calls).isEmpty();
        assertThat(webhook.calls).isEmpty();
    }

    @Test
    void readerMissingFromTheRepositoryReceivesNothing() {
        final ReaderDetails reader = readerChoosing(1, "email");
        when(readerRepository.findByReaderNumber(reader.getReaderNumber())).thenReturn(Optional.empty());
        outstanding(dueIn(reader, NOTICE_DAYS));

        serviceWithEnabled(email).checkDueAndOverdueLendings();

        assertThat(email.calls).isEmpty();
    }

    // The choice is read at each check, so a change applies to the next notification without restart (QAS6).
    @Test
    void changedChoiceAppliesToTheNextCheck() {
        final ReaderDetails reader = readerChoosing(1, "email");
        outstanding(dueIn(reader, NOTICE_DAYS));
        final LendingServiceImpl service = serviceWithEnabled(email, sms);

        service.checkDueAndOverdueLendings();
        reader.chooseNotificationChannels(Set.of("sms"));
        service.checkDueAndOverdueLendings();

        assertThat(email.calls).hasSize(1);
        assertThat(sms.calls).hasSize(1);
    }

    // trigger: which lendings produce which event (US6, US7)

    @Test
    void dueDateNoticeIsSentOnlyOnTheNoticeDay() {
        final ReaderDetails reader = readerChoosing(1, "email");
        final Lending dayBefore = dueIn(reader, NOTICE_DAYS - 1);
        final Lending noticeDay = dueIn(reader, NOTICE_DAYS);
        final Lending dayAfter = dueIn(reader, NOTICE_DAYS + 1);
        outstanding(dayBefore, noticeDay, dayAfter);

        serviceWithEnabled(email).checkDueAndOverdueLendings();

        assertThat(email.calls).extracting(call -> call.notification().lendingNumber())
                .containsExactly(noticeDay.getLendingNumber());
    }

    @Test
    void overdueLendingIsNotifiedWithItsDaysOverdueAndFine() {
        final ReaderDetails reader = readerChoosing(1, "email");
        final Lending overdue = dueIn(reader, -5);
        outstanding(overdue);

        serviceWithEnabled(email).checkDueAndOverdueLendings();

        assertThat(email.calls).containsExactly(new RecordingChannel.Call(reader.getReaderNumber(),
                LendingNotification.overdue(overdue.getLendingNumber(), "O Inspetor Max", TODAY.minusDays(5), 5, 1000)));
    }

    @Test
    void lendingDueTodayIsNeitherOverdueNorOnItsNoticeDay() {
        final ReaderDetails reader = readerChoosing(1, "email");
        outstanding(dueIn(reader, 0));

        serviceWithEnabled(email).checkDueAndOverdueLendings();

        assertThat(email.calls).isEmpty();
    }

    // failure behaviour (group decision): a failing channel is logged; other channels and readers continue

    @Test
    void failingChannelDoesNotStopTheOtherChannelsOrTheOtherReaders() {
        final ReaderDetails first = readerChoosing(1, "email", "sms");
        final ReaderDetails second = readerChoosing(2, "email");
        outstanding(dueIn(first, NOTICE_DAYS), dueIn(second, NOTICE_DAYS));
        email.failure = new IllegalStateException("email gateway unavailable");

        serviceWithEnabled(email, sms).checkDueAndOverdueLendings();

        assertThat(email.calls).extracting(RecordingChannel.Call::readerNumber)
                .containsExactly(first.getReaderNumber(), second.getReaderNumber());
        assertThat(sms.calls).extracting(RecordingChannel.Call::readerNumber)
                .containsExactly(first.getReaderNumber());
    }
}
