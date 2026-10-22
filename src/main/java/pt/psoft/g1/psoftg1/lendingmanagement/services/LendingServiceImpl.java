package pt.psoft.g1.psoftg1.lendingmanagement.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.exceptions.LendingForbiddenException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Fine;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.FineRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.LendingPolicy;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.LendingPolicySelector;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.LendingToRenew;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.PolicyDecision;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.ReaderLendingHistory;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.ReaderLendingHistoryProvider;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.shared.services.Page;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@PropertySource({"classpath:config/library.properties"})
public class LendingServiceImpl implements LendingService{
    private final LendingRepository lendingRepository;
    private final FineRepository fineRepository;
    private final BookRepository bookRepository;
    private final ReaderRepository readerRepository;
    private final Clock clock; // single time source, so tests can inject a fixed clock (QAS11)
    private final ReaderLendingHistoryProvider historyProvider;
    private final LendingPolicySelector policySelector;
    private final int fineValuePerDayInCents;

    // Explicit constructor (was Lombok + @Value field): every dependency, including the fine value, is passed in,
    // so the service can be built with test doubles and no Spring context (QAS11, TST-LP-05).
    public LendingServiceImpl(LendingRepository lendingRepository,
                              FineRepository fineRepository,
                              BookRepository bookRepository,
                              ReaderRepository readerRepository,
                              Clock clock,
                              ReaderLendingHistoryProvider historyProvider,
                              LendingPolicySelector policySelector,
                              @Value("${fineValuePerDayInCents}") int fineValuePerDayInCents) {
        this.lendingRepository = lendingRepository;
        this.fineRepository = fineRepository;
        this.bookRepository = bookRepository;
        this.readerRepository = readerRepository;
        this.clock = clock;
        this.historyProvider = historyProvider;
        this.policySelector = policySelector;
        this.fineValuePerDayInCents = fineValuePerDayInCents;
    }

    @Override
    public Optional<Lending> findByLendingNumber(String lendingNumber){
        return lendingRepository.findByLendingNumber(lendingNumber);
    }

    @Override
    public List<Lending> listByReaderNumberAndIsbn(String readerNumber, String isbn, Optional<Boolean> returned){
        List<Lending> lendings = lendingRepository.listByReaderNumberAndIsbn(readerNumber, isbn);
        if(returned.isEmpty()){
            return lendings;
        }else{
            for(int i = 0; i < lendings.size(); i++){
                if ((lendings.get(i).getReturnedDate() == null) == returned.get()){
                    lendings.remove(i);
                    i--;
                }
            }
        }
        return lendings;
    }

    @Override
    public Lending create(final CreateLendingRequest resource) {
        // The service only orchestrates: facts, policy choice and the rules live in the policy package (EXT-LP-05).
        final ReaderLendingHistory history = historyProvider.historyOf(resource.getReaderNumber());
        final LendingPolicy policy = policySelector.select(history);
        final PolicyDecision decision = policy.evaluateNewLending(history);
        // The reason travels in the exception so the REST 403 tells the reader why (US13).
        if (!decision.allowed()) {
            throw new LendingForbiddenException(decision.reason().message());
        }

        final var b = bookRepository.findByIsbn(resource.getIsbn())
                .orElseThrow(() -> new NotFoundException("Book not found"));
        final var r = readerRepository.findByReaderNumber(resource.getReaderNumber())
                .orElseThrow(() -> new NotFoundException("Reader not found"));
        int seq = lendingRepository.getCountFromCurrentYear()+1;
        // Period comes from the decision and the date from the snapshot, so the lending matches what was evaluated.
        final Lending l = new Lending(b,r,seq, decision.periodDays(), fineValuePerDayInCents, history.referenceDate());

        return lendingRepository.save(l);
    }

    @Override
    public Lending setReturned(final String lendingNumber, final SetLendingReturnedRequest resource, final long desiredVersion) {

        var lending = lendingRepository.findByLendingNumber(lendingNumber)
                .orElseThrow(() -> new NotFoundException("Cannot update lending with this lending number"));

        final LocalDate today = LocalDate.now(clock); // same date for the return and the fine
        lending.setReturned(desiredVersion, resource.getCommentary(), today);

        if(lending.getDaysDelayed(today) > 0){
            final var fine = new Fine(lending, today);
            fineRepository.save(fine);
        }

        return lendingRepository.save(lending);
    }

    @Override
    public Lending renew(final String lendingNumber, final long desiredVersion) {
        final var lending = lendingRepository.findByLendingNumber(lendingNumber)
                .orElseThrow(() -> new NotFoundException("Cannot renew lending with this lending number"));

        // Checked before asking a policy: a returned lending has nothing to renew, whatever the policy is.
        if (lending.getReturnedDate() != null) {
            throw new LendingForbiddenException("A returned lending cannot be renewed");
        }

        // Selection is repeated at renewal, so a reader whose history changed is evaluated by the current policy (LP-SEL2).
        final ReaderLendingHistory history = historyProvider.historyOf(lending.getReaderDetails().getReaderNumber());
        final LendingPolicy policy = policySelector.select(history);
        final boolean overdue = lending.getDaysDelayed(history.referenceDate()) > 0;
        final PolicyDecision decision = policy.evaluateRenewal(new LendingToRenew(overdue, lending.getRenewals()), history);
        if (!decision.allowed()) {
            throw new LendingForbiddenException(decision.reason().message());
        }

        lending.renew(desiredVersion, decision.periodDays(), history.referenceDate());
        return lendingRepository.save(lending);
    }

    @Override
    public Double getAverageDuration(){
        Double avg = lendingRepository.getAverageDuration();
        return Double.valueOf(String.format(Locale.US,"%.1f", avg));
    }

    @Override
    public List<Lending> getOverdue(Page page) {
        if (page == null) {
            page = new Page(1, 10);
        }
        return lendingRepository.getOverdue(page);
    }

    @Override
    public Double getAvgLendingDurationByIsbn(String isbn){
        Double avg = lendingRepository.getAvgLendingDurationByIsbn(isbn);
        return Double.valueOf(String.format(Locale.US,"%.1f", avg));
    }

    @Override
    public List<Lending> searchLendings(Page page, SearchLendingQuery query){
        LocalDate startDate = null;
        LocalDate endDate = null;

        if (page == null) {
            page = new Page(1, 10);
        }
        if (query == null)
            query = new SearchLendingQuery("",
                    "",
                    null,
                    LocalDate.now(clock).minusDays(10L).toString(),
                    null);

        try {
            if(query.getStartDate()!=null)
                startDate = LocalDate.parse(query.getStartDate());
            if(query.getEndDate()!=null)
                endDate = LocalDate.parse(query.getEndDate());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Expected format is YYYY-MM-DD");
        }

        return lendingRepository.searchLendings(page,
                query.getReaderNumber(),
                query.getIsbn(),
                query.getReturned(),
                startDate,
                endDate);

    }



}
