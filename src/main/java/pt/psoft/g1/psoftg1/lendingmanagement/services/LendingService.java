package pt.psoft.g1.psoftg1.lendingmanagement.services;

import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.shared.services.Page;

import java.util.List;
import java.util.Optional;

public interface LendingService {
    /**
     * @param lendingNumber
     * @return {@code Optional<Lending>}
     */
    Optional<Lending> findByLendingNumber(String lendingNumber);
    /**
     * @param readerNumber - Reader Number of the Reader associated with the lending
     * @param isbn         - ISBN of the book associated with the lending
     * @param returned     - Wether it's intended to filter by the return status of a lending
     * @return {@code Iterable<Lending>}
     */
    List<Lending> listByReaderNumberAndIsbn(String readerNumber, String isbn, Optional<Boolean> returned);
    Lending create(CreateLendingRequest resource); //No ID passed, as it is auto generated
    Lending setReturned(String id, SetLendingReturnedRequest resource, long desiredVersion);
    // Renewal is evaluated by the policy selected for the reader at request time (US14, LP-SEL2).
    Lending renew(String lendingNumber, long desiredVersion);
    Double getAverageDuration();
    List<Lending> getOverdue(Page page);
    Double getAvgLendingDurationByIsbn(String isbn);
    List<Lending> searchLendings(Page page, SearchLendingQuery request);


}
