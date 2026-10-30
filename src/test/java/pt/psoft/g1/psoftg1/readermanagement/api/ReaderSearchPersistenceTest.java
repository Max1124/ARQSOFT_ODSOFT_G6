package pt.psoft.g1.psoftg1.readermanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.testutils.ApiIntegrationTestSupport;
import pt.psoft.g1.psoftg1.readermanagement.services.SearchReadersQuery;
import pt.psoft.g1.psoftg1.shared.services.Page;

/** Collaboration SUT: real repository criteria query, domain and transactional test database. */
class ReaderSearchPersistenceTest extends ApiIntegrationTestSupport {
    @Test void searchUsesPartialNamesExactEmailsAndPhones() {
        // Arrange
        var first = newReader("first-search@example.org");
        var second = newReader("second-search@example.org");
        var page = new Page(1, 20);
        // Act / Assert
        assertThat(readerRepository.searchReaderDetails(page,
                new SearchReadersQuery(null, null, "first-search@example.org")))
                .extracting(r -> r.getReader().getUsername()).containsExactly("first-search@example.org");
        assertThat(readerRepository.searchReaderDetails(page,
                new SearchReadersQuery(null, null, "first-search"))).isEmpty();
        assertThat(readerRepository.searchReaderDetails(page,
                new SearchReadersQuery(null, first.getPhoneNumber(), null)))
                .extracting(r -> r.getPhoneNumber()).contains(first.getPhoneNumber());
        assertThat(readerRepository.searchReaderDetails(page,
                new SearchReadersQuery(first.getReader().getName().getName(), null, null)))
                .extracting(r -> r.getReader().getUsername()).contains("first-search@example.org");
        // Existing query uses OR: unmatched email must not cancel a matching phone.
        assertThat(readerRepository.searchReaderDetails(page,
                new SearchReadersQuery(null, first.getPhoneNumber(), "absent@example.org")))
                .extracting(r -> r.getReader().getUsername()).contains("first-search@example.org");
        assertThat(readerRepository.searchReaderDetails(new Page(1,1),
                new SearchReadersQuery("", "", ""))).hasSize(1);
        assertThat(readerRepository.searchReaderDetails(new Page(100,20),
                new SearchReadersQuery("", "", ""))).isEmpty();
    }
}
