package pt.psoft.g1.psoftg1.lendingmanagement.services.policy;

import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** Builds the read-only {@link ReaderLendingHistory} that policies and selection rules evaluate (EXT-LP-04). */
@Service
public class ReaderLendingHistoryProvider {
    private final LendingRepository lendingRepository;
    private final Clock clock;
    private final PolicySelectionParameters selectionParameters;

    // Repository access and the clock stay in this one class so policies and rules never touch them (CON-LP-4).
    public ReaderLendingHistoryProvider(LendingRepository lendingRepository, Clock clock,
                                        PolicySelectionParameters selectionParameters) {
        this.lendingRepository = lendingRepository;
        this.clock = clock;
        this.selectionParameters = selectionParameters;
    }

    public ReaderLendingHistory historyOf(String readerNumber) {
        // Read once: the same "today" defines overdue and the window, so the snapshot is internally consistent.
        final LocalDate today = LocalDate.now(clock);

        final List<Lending> outstanding = lendingRepository.listOutstandingByReaderNumber(readerNumber);
        final long overdue = outstanding.stream().filter(l -> l.getDaysDelayed(today) > 0).count();

        // Window start is HistoryPeriod months back, boundaries inclusive (design decision on the history window).
        final LocalDate windowStart = today.minusMonths(selectionParameters.historyPeriodMonths());
        final List<Lending> completed = lendingRepository.listReturnedByReaderNumberBetween(readerNumber, windowStart, today);
        // For a returned lending getDaysDelayed ignores today, so 0 means it was returned on or before its limit date.
        final long onTime = completed.stream().filter(l -> l.getDaysDelayed(today) == 0).count();

        return new ReaderLendingHistory(today, outstanding.size(), (int) overdue, completed.size(), (int) onTime);
    }
}
