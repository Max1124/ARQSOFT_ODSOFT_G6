package pt.psoft.g1.psoftg1.lendingmanagement.model;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LendingTest {
    // Fixed date so results do not depend on the day the tests run.
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);
    // Constant instead of @Value: this unit test no longer needs Spring or library.properties.
    private static final int LENDING_DURATION = 15;
    private static final int FINE_PER_DAY = 200;
    private static final ArrayList<Author> authors = new ArrayList<>();
    private static Book book;
    private static ReaderDetails readerDetails;

    private static Lending newLending() {
        return new Lending(book, readerDetails, 1, LENDING_DURATION, FINE_PER_DAY, TODAY);
    }

    @BeforeAll
    public static void setup(){
        Author author = new Author("Manuel Antonio Pina",
                "Manuel António Pina foi um jornalista e escritor português, premiado em 2011 com o Prémio Camões",
                null);
        authors.add(author);
        book = new Book("9782826012092",
                "O Inspetor Max",
                "conhecido pastor-alemão que trabalha para a Judiciária, vai ser fundamental para resolver um importante caso de uma rede de malfeitores que quer colocar uma bomba num megaconcerto de uma ilustre cantora",
                new Genre("Romance"),
                authors,
                null);
        readerDetails = new ReaderDetails(1,
                Reader.newReader("manuel@gmail.com", "Manuelino123!", "Manuel Sarapinto das Coives"),
                "2000-01-01",
                "919191919",
                true,
                true,
                true,
                null,
                null);
    }

    @Test
    void ensureBookNotNull(){
        assertThrows(IllegalArgumentException.class, () -> new Lending(null, readerDetails, 1, LENDING_DURATION, FINE_PER_DAY, TODAY));
    }

    @Test
    void ensureReaderNotNull(){
        assertThrows(IllegalArgumentException.class, () -> new Lending(book, null, 1, LENDING_DURATION, FINE_PER_DAY, TODAY));
    }

    @Test
    void ensureValidReaderNumber(){
        assertThrows(IllegalArgumentException.class, () -> new Lending(book, readerDetails, -1, LENDING_DURATION, FINE_PER_DAY, TODAY));
    }

    @Test
    void testSetReturned(){
        Lending lending = newLending();
        lending.setReturned(0, null, TODAY);
        assertEquals(TODAY, lending.getReturnedDate());
    }

    @Test
    void testGetDaysDelayed(){
        assertEquals(0, newLending().getDaysDelayed(TODAY));
    }

    @Test
    void testGetDaysUntilReturn(){
        assertEquals(Optional.of(LENDING_DURATION), newLending().getDaysUntilReturn(TODAY));
    }

    @Test
    void testGetDaysOverDue(){
        assertEquals(Optional.empty(), newLending().getDaysOverdue(TODAY));
    }

    @Test
    void lendingIsNotOverdueOnItsLimitDate(){
        Lending lending = newLending();
        LocalDate limit = TODAY.plusDays(LENDING_DURATION);
        assertEquals(0, lending.getDaysDelayed(limit));
        assertEquals(Optional.of(0), lending.getDaysUntilReturn(limit));
        assertEquals(Optional.empty(), lending.getFineValueInCents(limit));
    }

    @Test
    void lendingIsOverdueTheDayAfterItsLimitDate(){
        Lending lending = newLending();
        LocalDate afterLimit = TODAY.plusDays(LENDING_DURATION + 1);
        assertEquals(1, lending.getDaysDelayed(afterLimit));
        assertEquals(Optional.of(1), lending.getDaysOverdue(afterLimit));
        assertEquals(Optional.empty(), lending.getDaysUntilReturn(afterLimit));
        assertEquals(Optional.of(FINE_PER_DAY), lending.getFineValueInCents(afterLimit));
    }

    @Test
    void fineGrowsWithEachDayOverdue(){
        assertEquals(Optional.of(5 * FINE_PER_DAY), newLending().getFineValueInCents(TODAY.plusDays(LENDING_DURATION + 5)));
    }

    @Test
    void returnedOnTheLimitDateIsNotDelayed(){
        Lending lending = newLending();
        lending.setReturned(0, null, TODAY.plusDays(LENDING_DURATION));
        assertEquals(0, lending.getDaysDelayed(TODAY.plusDays(100)));
    }

    @Test
    void returnedLateKeepsItsDaysDelayedAfterwards(){
        Lending lending = newLending();
        lending.setReturned(0, null, TODAY.plusDays(LENDING_DURATION + 3));
        assertEquals(3, lending.getDaysDelayed(TODAY.plusDays(100)));
    }

    @Test
    void returnedLendingHasNoDaysUntilReturn(){
        Lending lending = newLending();
        lending.setReturned(0, null, TODAY);
        assertEquals(Optional.empty(), lending.getDaysUntilReturn(TODAY));
    }

    @Test
    void cannotReturnTwice(){
        Lending lending = newLending();
        lending.setReturned(0, null, TODAY);
        assertThrows(IllegalArgumentException.class, () -> lending.setReturned(0, null, TODAY));
    }

    @Test
    void testGetTitle() {
        assertEquals("O Inspetor Max", newLending().getTitle());
    }

    @Test
    void testGetLendingNumber() {
        assertEquals(TODAY.getYear() + "/1", newLending().getLendingNumber());
    }

    @Test
    void testGetBook() {
        assertEquals(book, newLending().getBook());
    }

    @Test
    void testGetReaderDetails() {
        assertEquals(readerDetails, newLending().getReaderDetails());
    }

    @Test
    void testGetStartDate() {
        assertEquals(TODAY, newLending().getStartDate());
    }

    @Test
    void testGetLimitDate() {
        assertEquals(TODAY.plusDays(LENDING_DURATION), newLending().getLimitDate());
    }

    @Test
    void testGetReturnedDate() {
        assertNull(newLending().getReturnedDate());
    }

}
