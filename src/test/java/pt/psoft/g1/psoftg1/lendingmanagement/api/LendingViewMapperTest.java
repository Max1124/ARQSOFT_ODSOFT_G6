package pt.psoft.g1.psoftg1.lendingmanagement.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Transparent-box test of the view mapping, with a fixed clock so days left, overdue and fine are deterministic. */
class LendingViewMapperTest {
    private static final LocalDate START = LocalDate.of(2026, 3, 1);
    private static final int PERIOD = 15;
    private static final int FINE_PER_DAY = 200;

    private static Book book;
    private static ReaderDetails readerDetails;

    private LendingViewMapper mapper;

    @BeforeAll
    static void setupData() {
        book = new Book("9782826012092",
                "O Inspetor Max",
                "conhecido pastor-alemão que trabalha para a Judiciária",
                new Genre("Romance"),
                List.of(new Author("Manuel Antonio Pina", "Jornalista e escritor português", null)),
                null);
        readerDetails = new ReaderDetails(1,
                Reader.newReader("manuel@gmail.com", "Manuelino123!", "Manuel Sarapinto das Coives"),
                "2000-01-01", "919191919", true, true, true, null, null);
    }

    @BeforeEach
    void setUp() {
        // Links are built from the current request, so a request context must exist.
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        mapper = new LendingViewMapperImpl();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void today(LocalDate date) {
        final var clock = Clock.fixed(Instant.parse(date + "T12:00:00Z"), ZoneOffset.UTC);
        ReflectionTestUtils.setField(mapper, "clock", clock);
    }

    private static Lending newLending() {
        return new Lending(book, readerDetails, 1, PERIOD, FINE_PER_DAY, START);
    }

    @Test
    void outstandingLendingShowsDaysUntilReturnAndNoFine() {
        today(START.plusDays(5));

        final var view = mapper.toLendingView(newLending());

        assertEquals("2026/1", view.getLendingNumber());
        assertEquals("O Inspetor Max", view.getBookTitle());
        assertEquals(START, view.getStartDate());
        assertEquals(START.plusDays(PERIOD), view.getLimitDate());
        assertEquals(10, view.getDaysUntilReturn());
        assertNull(view.getDaysOverdue());
        assertNull(view.getFineValueInCents());
        assertNull(view.getReturnedDate());
    }

    @Test
    void overdueLendingShowsDaysOverdueAndFine() {
        today(START.plusDays(PERIOD + 4));

        final var view = mapper.toLendingView(newLending());

        assertNull(view.getDaysUntilReturn());
        assertEquals(4, view.getDaysOverdue());
        assertEquals(4 * FINE_PER_DAY, view.getFineValueInCents());
    }

    @Test
    void viewHasLinksToLendingBookAndReader() {
        today(START);

        final var links = mapper.toLendingView(newLending()).get_links();

        assertEquals("http://localhost/api/lendings/2026/1", links.getSelf().get("href"));
        assertEquals("http://localhost/api/books/9782826012092", links.getBook().get("href"));
        assertEquals("http://localhost/api/readers/" + readerDetails.getReaderNumber(), links.getReader().get("href"));
    }

    @Test
    void listMappingKeepsTheOrderOfTheLendings() {
        today(START);
        final var first = newLending();
        final var second = new Lending(book, readerDetails, 2, PERIOD, FINE_PER_DAY, START);

        final var views = mapper.toLendingView(List.of(first, second));

        assertEquals(List.of("2026/1", "2026/2"), views.stream().map(LendingView::getLendingNumber).toList());
    }

    @Test
    void averageDurationIsWrappedInItsView() {
        final var view = mapper.toLendingsAverageDurationView(12.5);

        assertEquals(12.5, view.getLendingsAverageDuration());
    }
}
