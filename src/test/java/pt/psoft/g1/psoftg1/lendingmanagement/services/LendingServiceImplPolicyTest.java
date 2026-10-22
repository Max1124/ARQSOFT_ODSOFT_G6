package pt.psoft.g1.psoftg1.lendingmanagement.services;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.exceptions.LendingForbiddenException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.FineRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.LendingPolicy;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.LendingPolicySelector;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.PolicyDecision;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.ReaderLendingHistory;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.ReaderLendingHistoryProvider;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.RejectionReason;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Class-level test of the service: every collaborator is a double, so only delegation is under test (CON-TST-10). */
class LendingServiceImplPolicyTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);
    private static final String READER_NUMBER = "2026/1";
    private static final ReaderLendingHistory HISTORY = new ReaderLendingHistory(TODAY, 1, 0, 0, 0);
    private static Book book;
    private static ReaderDetails reader;

    private final LendingRepository lendingRepository = mock(LendingRepository.class);
    private final FineRepository fineRepository = mock(FineRepository.class);
    private final BookRepository bookRepository = mock(BookRepository.class);
    private final ReaderRepository readerRepository = mock(ReaderRepository.class);
    private final ReaderLendingHistoryProvider historyProvider = mock(ReaderLendingHistoryProvider.class);
    private final LendingPolicySelector selector = mock(LendingPolicySelector.class);
    private final LendingPolicy chosenPolicy = mock(LendingPolicy.class);
    private final Clock clock = Clock.fixed(TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    private LendingServiceImpl service;

    @BeforeAll
    static void fixtures() {
        book = new Book("9782826012092", "O Inspetor Max", "descricao",
                new Genre("Romance"), List.of(new Author("Manuel Antonio Pina", "bio", null)), null);
        reader = new ReaderDetails(1, Reader.newReader("manuel@gmail.com", "Manuelino123!", "Manuel Sarapinto das Coives"),
                "2000-01-01", "919191919", true, true, true, null, null);
    }

    @BeforeEach
    void wire() {
        service = new LendingServiceImpl(lendingRepository, fineRepository, bookRepository, readerRepository,
                clock, historyProvider, selector, 200);
        when(historyProvider.historyOf(READER_NUMBER)).thenReturn(HISTORY);
        when(selector.select(HISTORY)).thenReturn(chosenPolicy);
        when(bookRepository.findByIsbn("9782826012092")).thenReturn(Optional.of(book));
        when(readerRepository.findByReaderNumber(READER_NUMBER)).thenReturn(Optional.of(reader));
        when(lendingRepository.getCountFromCurrentYear()).thenReturn(4);
        when(lendingRepository.save(any(Lending.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static CreateLendingRequest request() {
        return new CreateLendingRequest("9782826012092", READER_NUMBER);
    }

    @Test
    void allowedDecisionCreatesTheLendingWithTheDecidedPeriod() {
        when(chosenPolicy.evaluateNewLending(HISTORY)).thenReturn(PolicyDecision.allow(21));

        Lending created = service.create(request());

        assertThat(created.getStartDate()).isEqualTo(TODAY);
        assertThat(created.getLimitDate()).isEqualTo(TODAY.plusDays(21));
        assertThat(created.getLendingNumber()).isEqualTo("2026/5");
        verify(lendingRepository).save(created);
    }

    @Test
    void lendingCarriesTheConfiguredFineValue() {
        when(chosenPolicy.evaluateNewLending(HISTORY)).thenReturn(PolicyDecision.allow(15));

        assertThat(service.create(request()).getFineValuePerDayInCents()).isEqualTo(200);
    }

    @Test
    void rejectedDecisionThrowsWithThePolicyReasonAndSavesNothing() {
        when(chosenPolicy.evaluateNewLending(HISTORY)).thenReturn(PolicyDecision.reject(RejectionReason.MAX_ACTIVE_LENDINGS));

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(LendingForbiddenException.class)
                .hasMessage(RejectionReason.MAX_ACTIVE_LENDINGS.message());
        verify(lendingRepository, never()).save(any());
    }

    @Test
    void rejectedDecisionDoesNotTouchBookOrReaderRepositories() {
        when(chosenPolicy.evaluateNewLending(HISTORY)).thenReturn(PolicyDecision.reject(RejectionReason.OVERDUE_LENDING));

        assertThatThrownBy(() -> service.create(request())).isInstanceOf(LendingForbiddenException.class);
        verifyNoInteractions(bookRepository, readerRepository);
    }

    @Test
    void historyIsBuiltForTheRequestedReaderAndPassedToTheSelector() {
        when(chosenPolicy.evaluateNewLending(HISTORY)).thenReturn(PolicyDecision.allow(15));

        service.create(request());

        verify(historyProvider).historyOf(READER_NUMBER);
        verify(selector).select(HISTORY);
    }

    @Test
    void unknownBookIsReportedAfterTheDecisionAllowed() {
        when(chosenPolicy.evaluateNewLending(HISTORY)).thenReturn(PolicyDecision.allow(15));
        when(bookRepository.findByIsbn("9782826012092")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request())).isInstanceOf(NotFoundException.class);
        verify(lendingRepository, never()).save(any());
    }
}
