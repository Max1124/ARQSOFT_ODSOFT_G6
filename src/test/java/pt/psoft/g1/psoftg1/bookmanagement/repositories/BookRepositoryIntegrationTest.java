package pt.psoft.g1.psoftg1.bookmanagement.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.services.SearchBooksQuery;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.shared.services.Page;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Integration test of the {@link BookRepository} queries against the in-memory database; each test is rolled back. */
@Transactional
@SpringBootTest
class BookRepositoryIntegrationTest {
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private AuthorRepository authorRepository;
    @Autowired
    private GenreRepository genreRepository;
    @Autowired
    private ReaderRepository readerRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LendingRepository lendingRepository;

    private Author pina;
    private Author alberto;
    private Genre children;
    private Genre fantasy;
    private Book inspector;
    private Book encantos;

    @BeforeEach
    void setUp() {
        pina = authorRepository.save(new Author("Manuel Antonio Pina", "Manuel António Pina foi um jornalista e escritor português.", null));
        alberto = authorRepository.save(new Author("João Alberto", "O João Alberto nasceu em Chaves e foi pedreiro.", null));
        children = genreRepository.save(new Genre("Infantil"));
        fantasy = genreRepository.save(new Genre("Fantasia"));
        inspector = bookRepository.save(new Book("9782826012092", "O Inspetor Max", "Um pastor-alemão que trabalha para a Judiciária",
                children, List.of(pina), null));
        encantos = bookRepository.save(new Book("9789722025065", "Encantos de contar", "Contos fantásticos",
                fantasy, List.of(alberto), null));
    }

    @Test
    void findByIsbnReturnsTheBook() {
        assertThat(bookRepository.findByIsbn("9782826012092")).contains(inspector);
    }

    @Test
    void findByUnknownIsbnIsEmpty() {
        assertThat(bookRepository.findByIsbn("9780000000002")).isEmpty();
    }

    @Test
    void findByTitleMatchesAnywhereInTheTitle() {
        assertThat(bookRepository.findByTitle("Inspetor")).containsExactly(inspector);
        assertThat(bookRepository.findByTitle("zzz")).isEmpty();
    }

    @Test
    void findByGenreMatchesAPartOfTheGenreName() {
        assertThat(bookRepository.findByGenre("Fanta")).containsExactly(encantos);
        assertThat(bookRepository.findByGenre("Infantil")).containsExactly(inspector);
    }

    @Test
    void findByAuthorNameMatchesThePatternGivenByTheCaller() {
        assertThat(bookRepository.findByAuthorName("Manuel%")).containsExactly(inspector);
        assertThat(bookRepository.findByAuthorName("Ninguém%")).isEmpty();
    }

    @Test
    void findBooksByAuthorNumberReturnsOnlyThatAuthorsBooks() {
        assertThat(bookRepository.findBooksByAuthorNumber(alberto.getAuthorNumber())).containsExactly(encantos);
    }

    @Test
    void searchWithoutCriteriaReturnsAllBooksOrderedByTitle() {
        final var found = bookRepository.searchBooks(new Page(1, 10), new SearchBooksQuery("", "", ""));

        assertThat(found).containsSubsequence(encantos, inspector);
    }

    @Test
    void searchFiltersByTitleGenreAndAuthorPrefixesTogether() {
        assertThat(bookRepository.searchBooks(new Page(1, 10), new SearchBooksQuery("O Insp", "", ""))).containsExactly(inspector);
        assertThat(bookRepository.searchBooks(new Page(1, 10), new SearchBooksQuery("", "Fant", ""))).containsExactly(encantos);
        assertThat(bookRepository.searchBooks(new Page(1, 10), new SearchBooksQuery("", "", "João"))).containsExactly(encantos);
        assertThat(bookRepository.searchBooks(new Page(1, 10), new SearchBooksQuery("O Insp", "Fant", ""))).isEmpty();
    }

    @Test
    void searchPaginates() {
        final var query = new SearchBooksQuery("", "", "");
        final var first = bookRepository.searchBooks(new Page(1, 1), query);
        final var second = bookRepository.searchBooks(new Page(2, 1), query);

        assertThat(first).hasSize(1);
        assertThat(second).hasSize(1);
        assertThat(first).doesNotContainAnyElementsOf(second);
    }

    @Test
    void top5CountsLendingsOfTheLastYearAndOrdersByCount() {
        final var user = userRepository.save(Reader.newReader("manuel@gmail.com", "Manuelino123!", "Manuel Sarapinto das Coives"));
        final var reader = readerRepository.save(new ReaderDetails(1, user, "2000-01-01", "919191919", true, true, true, null, null));
        final var today = LocalDate.now();
        lendingRepository.save(new Lending(encantos, reader, 1, 14, 50, today));
        lendingRepository.save(new Lending(encantos, reader, 2, 14, 50, today));
        lendingRepository.save(new Lending(inspector, reader, 3, 14, 50, today));
        // Too old to count: outside the one-year window.
        lendingRepository.save(Lending.newBootstrappingLending(inspector, reader, today.getYear() - 2, 4,
                today.minusYears(2), today.minusYears(2).plusDays(5), 14, 50));

        final var top = bookRepository.findTop5BooksLent(today.minusYears(1), PageRequest.of(0, 5)).getContent();

        assertThat(top).hasSize(2);
        assertThat(top.get(0).getBook()).isEqualTo(encantos);
        assertThat(top.get(0).getLendingCount()).isEqualTo(2);
        assertThat(top.get(1).getBook()).isEqualTo(inspector);
        assertThat(top.get(1).getLendingCount()).isEqualTo(1);
    }
}
