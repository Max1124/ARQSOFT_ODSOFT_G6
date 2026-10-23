package pt.psoft.g1.psoftg1.lendingmanagement.model;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Transparent-box tests of the fine computation (value = days of delay x value per day at creation). */
class FineTest {
    private static final LocalDate START = LocalDate.of(2026, 3, 1);
    private static final int PERIOD = 15;
    private static final int FINE_PER_DAY = 200;
    private static final LocalDate DUE_DATE = START.plusDays(PERIOD);

    private static Book book;
    private static ReaderDetails readerDetails;

    @BeforeAll
    static void setup() {
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

    private static Lending newLending() {
        return new Lending(book, readerDetails, 1, PERIOD, FINE_PER_DAY, START);
    }

    @Test
    void fineValueIsDaysOfDelayTimesValuePerDay() {
        final var lending = newLending();

        final var fine = new Fine(lending, DUE_DATE.plusDays(3));

        assertEquals(3 * FINE_PER_DAY, fine.getCentsValue());
    }

    @Test
    void fineKeepsTheValuePerDayOfTheLending() {
        final var lending = newLending();

        final var fine = new Fine(lending, DUE_DATE.plusDays(1));

        assertEquals(FINE_PER_DAY, fine.getFineValuePerDayInCents());
        assertSame(lending, fine.getLending());
    }

    @Test
    void oneDayOfDelayIsTheSmallestFine() {
        final var fine = new Fine(newLending(), DUE_DATE.plusDays(1));

        assertEquals(FINE_PER_DAY, fine.getCentsValue());
    }

    @Test
    void lendingDueTodayHasNoFine() {
        final var lending = newLending();

        assertThrows(IllegalArgumentException.class, () -> new Fine(lending, DUE_DATE));
    }

    @Test
    void lendingNotYetDueHasNoFine() {
        final var lending = newLending();

        assertThrows(IllegalArgumentException.class, () -> new Fine(lending, START));
    }

    @Test
    void lendingMustNotBeNull() {
        assertThrows(NullPointerException.class, () -> new Fine(null, DUE_DATE));
    }
}
