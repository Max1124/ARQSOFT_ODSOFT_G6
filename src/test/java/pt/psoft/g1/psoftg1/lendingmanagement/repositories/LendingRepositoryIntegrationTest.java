package pt.psoft.g1.psoftg1.lendingmanagement.repositories;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.shared.services.Page;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
@SpringBootTest
public class LendingRepositoryIntegrationTest {

    @Autowired
    private LendingRepository lendingRepository;
    @Autowired
    private ReaderRepository readerRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private GenreRepository genreRepository;
    @Autowired
    private AuthorRepository authorRepository;

    private Lending lending;
    private ReaderDetails readerDetails;
    private Reader reader;
    private Book book;
    private Author author;
    private Genre genre;

    @BeforeEach
    public void setUp() {
        author = new Author("Manuel Antonio Pina",
                "Manuel António Pina foi um jornalista e escritor português, premiado em 2011 com o Prémio Camões",
                null);
        authorRepository.save(author);

        genre = new Genre("Género");
        genreRepository.save(genre);

        List<Author> authors = List.of(author);
        book = new Book("9782826012092",
                "O Inspetor Max",
                "conhecido pastor-alemão que trabalha para a Judiciária, vai ser fundamental para resolver um importante caso de uma rede de malfeitores que quer colocar uma bomba num megaconcerto de uma ilustre cantora",
                genre,
                authors,
                null);
        bookRepository.save(book);

        reader = Reader.newReader("manuel@gmail.com", "Manuelino123!", "Manuel Sarapinto das Coives");
        userRepository.save(reader);

        readerDetails = new ReaderDetails(1,
                reader,
                "2000-01-01",
                "919191919",
                true,
                true,
                true,
                null,
                null);
        readerRepository.save(readerDetails);

        // Create and save the lending
        lending = Lending.newBootstrappingLending(book,
                readerDetails,
                LocalDate.now().getYear(),
                999,
                LocalDate.of(LocalDate.now().getYear(), 1,1),
                LocalDate.of(LocalDate.now().getYear(), 1,11),
                15,
                300);
        lendingRepository.save(lending);
    }

    @AfterEach
    public void tearDown(){
        lendingRepository.delete(lending);
        readerRepository.delete(readerDetails);
        userRepository.delete(reader);
        bookRepository.delete(book);
        genreRepository.delete(genre);
        authorRepository.delete(author);
    }

    @Test
    public void testSave() {
        Lending newLending = new Lending(lending.getBook(), lending.getReaderDetails(), 2, 14, 50, LocalDate.now());
        Lending savedLending = lendingRepository.save(newLending);
        assertThat(savedLending).isNotNull();
        assertThat(savedLending.getLendingNumber()).isEqualTo(newLending.getLendingNumber());
        lendingRepository.delete(savedLending);
    }

    @Test
    public void testFindByLendingNumber() {
        String ln = lending.getLendingNumber();
        Optional<Lending> found = lendingRepository.findByLendingNumber(ln);
        assertThat(found).isPresent();
        assertThat(found.get().getLendingNumber()).isEqualTo(ln);
    }

    @Test
    public void testListByReaderNumberAndIsbn() {
        List<Lending> lendings = lendingRepository.listByReaderNumberAndIsbn(lending.getReaderDetails().getReaderNumber(), lending.getBook().getIsbn());
        assertThat(lendings).isNotEmpty();
        assertThat(lendings).contains(lending);
    }

    @Test
    public void testGetCountFromCurrentYear() {
        int count = lendingRepository.getCountFromCurrentYear();
        assertThat(count).isEqualTo(1);
        var lending2 = Lending.newBootstrappingLending(book,
                readerDetails,
                LocalDate.now().getYear(),
                998,
                LocalDate.of(LocalDate.now().getYear(), 5,31),
                null,
                15,
                300);
        lendingRepository.save(lending2);
        count = lendingRepository.getCountFromCurrentYear();
        assertThat(count).isEqualTo(2);
    }

    // Window bounds are inclusive: a lending returned exactly on either bound must be counted by the history provider.
    @Test
    public void testListReturnedBetweenIncludesBothBoundaries() {
        LocalDate returned = lending.getReturnedDate();
        List<Lending> found = lendingRepository.listReturnedByReaderNumberBetween(
                lending.getReaderDetails().getReaderNumber(), returned, returned);
        assertThat(found).contains(lending);
    }

    @Test
    public void testListReturnedBetweenExcludesLendingsReturnedOutsideTheWindow() {
        String readerNumber = lending.getReaderDetails().getReaderNumber();
        LocalDate returned = lending.getReturnedDate();
        assertThat(lendingRepository.listReturnedByReaderNumberBetween(readerNumber, returned.plusDays(1), returned.plusMonths(1)))
                .doesNotContain(lending);
        assertThat(lendingRepository.listReturnedByReaderNumberBetween(readerNumber, returned.minusMonths(1), returned.minusDays(1)))
                .doesNotContain(lending);
    }

    @Test
    public void testListReturnedBetweenIgnoresLendingsNotYetReturned() {
        var outstanding = Lending.newBootstrappingLending(book,
                readerDetails,
                2024,
                997,
                LocalDate.of(2024, 5,31),
                null,
                15,
                300);
        lendingRepository.save(outstanding);
        assertThat(lendingRepository.listReturnedByReaderNumberBetween(
                lending.getReaderDetails().getReaderNumber(), LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1)))
                .doesNotContain(outstanding);
    }

    @Test
    public void testListOutstandingByReaderNumber() {
        var lending2 = Lending.newBootstrappingLending(book,
                readerDetails,
                2024,
                998,
                LocalDate.of(2024, 5,31),
                null,
                15,
                300);
        lendingRepository.save(lending2);
        List<Lending> outstandingLendings = lendingRepository.listOutstandingByReaderNumber(lending.getReaderDetails().getReaderNumber());
        assertThat(outstandingLendings).contains(lending2);
    }

    @Test
    public void testGetAverageDuration() {
        double lendingDuration1 = ChronoUnit.DAYS.between(lending.getStartDate(), lending.getReturnedDate());
        Double averageDuration = lendingRepository.getAverageDuration();
        assertNotNull(averageDuration);
        assertEquals(lendingDuration1, lendingRepository.getAverageDuration(), 0.001);

        var lending2 = lendingRepository.save(Lending.newBootstrappingLending(book,
                readerDetails,
                2024,
                998,
                LocalDate.of(2024, 2,1),
                LocalDate.of(2024, 4,4),
                15,
                300));
        double lendingDuration2 = ChronoUnit.DAYS.between(lending2.getStartDate(), lending2.getReturnedDate());
        double expectedAvg = (lendingDuration1 + lendingDuration2) / 2 ;
        assertEquals(expectedAvg, lendingRepository.getAverageDuration(), 0.001);

        var lending3 = lendingRepository.save(Lending.newBootstrappingLending(book,
                readerDetails,
                2024,
                997,
                LocalDate.of(2024, 3,1),
                LocalDate.of(2024, 4,25),
                15,
                300));
        double lendingDuration3 = ChronoUnit.DAYS.between(lending3.getStartDate(), lending3.getReturnedDate());
        expectedAvg = (lendingDuration1 + lendingDuration2 + lendingDuration3) / 3 ;
        assertEquals(expectedAvg, lendingRepository.getAverageDuration(), 0.001);

    }

    @Test
    public void testGetOverdue() {
        var returnedLateLending = lendingRepository.save(Lending.newBootstrappingLending(book,
                readerDetails,
                2024,
                998,
                LocalDate.of(2024, 1,1),
                LocalDate.of(2024, 2,1),
                15,
                300));
        var notReturnedLending = lendingRepository.save(Lending.newBootstrappingLending(book,
                readerDetails,
                2024,
                997,
                LocalDate.of(2024, 3,1),
                null,
                15,
                300));
        var notReturnedAndNotOverdueLending = lendingRepository.save(Lending.newBootstrappingLending(book,
                readerDetails,
                2024,
                996,
                LocalDate.of(LocalDate.now().getYear(), LocalDate.now().getMonth(),LocalDate.now().getDayOfMonth()),
                null,
                15,
                300));
        Page page = new Page(1, 10);
        List<Lending> overdueLendings = lendingRepository.getOverdue(page);
        assertThat(overdueLendings).doesNotContain(returnedLateLending);
        assertThat(overdueLendings).contains(notReturnedLending);
        assertThat(overdueLendings).doesNotContain(notReturnedAndNotOverdueLending);
    }

    @Test
    public void testGetAvgLendingDurationByIsbn() {
        // setUp lending lasted 10 days; the second one lasts 20, so the average for the book is 15.
        lendingRepository.save(Lending.newBootstrappingLending(book,
                readerDetails,
                2024,
                998,
                LocalDate.of(2024, 2, 1),
                LocalDate.of(2024, 2, 21),
                15,
                300));
        assertEquals(15.0, lendingRepository.getAvgLendingDurationByIsbn(book.getIsbn()), 0.001);
    }

    @Test
    public void testGetAvgLendingDurationByUnknownIsbnIsNull() {
        assertNull(lendingRepository.getAvgLendingDurationByIsbn("9780000000002"));
    }

    private Lending saveOutstanding2024() {
        return lendingRepository.save(Lending.newBootstrappingLending(book,
                readerDetails,
                2024,
                998,
                LocalDate.of(2024, 5, 31),
                null,
                15,
                300));
    }

    @Test
    public void testSearchLendingsFiltersByReturnedFlag() {
        var outstanding = saveOutstanding2024();
        String readerNumber = readerDetails.getReaderNumber();
        Page page = new Page(1, 10);

        assertThat(lendingRepository.searchLendings(page, readerNumber, null, true, null, null))
                .contains(lending).doesNotContain(outstanding);
        assertThat(lendingRepository.searchLendings(page, readerNumber, null, false, null, null))
                .contains(outstanding).doesNotContain(lending);
        assertThat(lendingRepository.searchLendings(page, readerNumber, null, null, null, null))
                .contains(lending, outstanding);
    }

    @Test
    public void testSearchLendingsFiltersByReaderNumberAndIsbn() {
        assertThat(lendingRepository.searchLendings(new Page(1, 10), readerDetails.getReaderNumber(), book.getIsbn(), null, null, null))
                .contains(lending);
        assertThat(lendingRepository.searchLendings(new Page(1, 10), "2000/999", null, null, null, null))
                .doesNotContain(lending);
        assertThat(lendingRepository.searchLendings(new Page(1, 10), null, "9780000000002", null, null, null))
                .doesNotContain(lending);
    }

    // Start-date bounds are inclusive on both sides.
    @Test
    public void testSearchLendingsFiltersByStartDateRangeInclusively() {
        var outstanding = saveOutstanding2024();
        LocalDate start = outstanding.getStartDate();

        assertThat(lendingRepository.searchLendings(new Page(1, 10), null, null, null, start, start))
                .contains(outstanding).doesNotContain(lending);
        assertThat(lendingRepository.searchLendings(new Page(1, 10), null, null, null, start.plusDays(1), null))
                .doesNotContain(outstanding);
        assertThat(lendingRepository.searchLendings(new Page(1, 10), null, null, null, null, start.minusDays(1)))
                .doesNotContain(outstanding);
    }

    @Test
    public void testSearchLendingsPaginatesInLendingNumberOrder() {
        var outstanding = saveOutstanding2024();

        List<Lending> first = lendingRepository.searchLendings(new Page(1, 1), readerDetails.getReaderNumber(), null, null, null, null);
        List<Lending> second = lendingRepository.searchLendings(new Page(2, 1), readerDetails.getReaderNumber(), null, null, null, null);

        assertThat(first).containsExactly(outstanding); // 2024/998 sorts before the current-year lending
        assertThat(second).containsExactly(lending);
    }
}
