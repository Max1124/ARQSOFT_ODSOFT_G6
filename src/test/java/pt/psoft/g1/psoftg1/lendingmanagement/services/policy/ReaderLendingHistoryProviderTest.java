package pt.psoft.g1.psoftg1.lendingmanagement.services.policy;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReaderLendingHistoryProviderTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);
    private static final String READER = "2026/1";
    private static final int PERIOD = 15;
    private static Book book;
    private static ReaderDetails reader;

    private final LendingRepository repository = mock(LendingRepository.class);
    private final Clock clock = Clock.fixed(TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    // History window of 6 months
    private final ReaderLendingHistoryProvider provider =
            new ReaderLendingHistoryProvider(repository, clock, new PolicySelectionParameters(6, 5, 90.0));

    @BeforeAll
    static void fixtures() {
        book = new Book("9782826012092", "O Inspetor Max", "descricao",
                new Genre("Romance"), List.of(new Author("Manuel Antonio Pina", "bio", null)), null);
        reader = new ReaderDetails(1, Reader.newReader("manuel@gmail.com", "Manuelino123!", "Manuel Sarapinto das Coives"),
                "2000-01-01", "919191919", true, true, true, null, null);
    }

    private static Lending lending(int seq, LocalDate start, LocalDate returned) {
        return Lending.newBootstrappingLending(book, reader, start.getYear(), seq, start, returned, PERIOD, 200);
    }

    @Test
    void readerWithNoLendingsHasAnEmptyHistory() {
        when(repository.listOutstandingByReaderNumber(READER)).thenReturn(List.of());
        when(repository.listReturnedByReaderNumberBetween(READER, TODAY.minusMonths(6), TODAY)).thenReturn(List.of());

        assertThat(provider.historyOf(READER)).isEqualTo(new ReaderLendingHistory(TODAY, 0, 0, 0, 0));
    }

    @Test
    void countsActiveAndOverdueLendings() {
        Lending overdue = lending(1, LocalDate.of(2026, 2, 1), null);   // limit 16 Feb, overdue
        Lending current = lending(2, LocalDate.of(2026, 3, 5), null);   // limit 20 Mar, not overdue
        when(repository.listOutstandingByReaderNumber(READER)).thenReturn(List.of(overdue, current));
        when(repository.listReturnedByReaderNumberBetween(READER, TODAY.minusMonths(6), TODAY)).thenReturn(List.of());

        ReaderLendingHistory history = provider.historyOf(READER);

        assertThat(history.activeLendings()).isEqualTo(2);
        assertThat(history.overdueLendings()).isEqualTo(1);
    }

    @Test
    void lendingDueTodayIsNotOverdue() {
        Lending dueToday = lending(1, TODAY.minusDays(PERIOD), null);
        when(repository.listOutstandingByReaderNumber(READER)).thenReturn(List.of(dueToday));
        when(repository.listReturnedByReaderNumberBetween(READER, TODAY.minusMonths(6), TODAY)).thenReturn(List.of());

        assertThat(provider.historyOf(READER).overdueLendings()).isZero();
    }

    @Test
    void countsCompletedAndOnTimeLendingsInTheWindow() {
        Lending onTime = lending(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 16));   // returned on the limit date
        Lending early = lending(3, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 5));     // returned before the limit date
        Lending late = lending(2, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 17));     // one day late
        when(repository.listOutstandingByReaderNumber(READER)).thenReturn(List.of());
        when(repository.listReturnedByReaderNumberBetween(READER, TODAY.minusMonths(6), TODAY)).thenReturn(List.of(onTime, early, late));

        ReaderLendingHistory history = provider.historyOf(READER);

        assertThat(history.completedInWindow()).isEqualTo(3);
        assertThat(history.onTimeInWindow()).isEqualTo(2);
    }

    @Test
    void windowStartsHistoryPeriodMonthsBeforeTodayAndEndsToday() {
        when(repository.listOutstandingByReaderNumber(READER)).thenReturn(List.of());
        when(repository.listReturnedByReaderNumberBetween(READER, LocalDate.of(2025, 9, 10), TODAY)).thenReturn(List.of());

        provider.historyOf(READER);

        verify(repository).listReturnedByReaderNumberBetween(READER, LocalDate.of(2025, 9, 10), TODAY);
    }

    @Test
    void snapshotCarriesTheReferenceDateFromTheClock() {
        when(repository.listOutstandingByReaderNumber(READER)).thenReturn(List.of());
        when(repository.listReturnedByReaderNumberBetween(READER, TODAY.minusMonths(6), TODAY)).thenReturn(List.of());

        assertThat(provider.historyOf(READER).referenceDate()).isEqualTo(TODAY);
    }
}
