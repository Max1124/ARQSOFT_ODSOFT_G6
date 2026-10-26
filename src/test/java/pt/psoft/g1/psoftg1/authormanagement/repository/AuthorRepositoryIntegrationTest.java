package pt.psoft.g1.psoftg1.authormanagement.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorLendingView;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Integration test of the {@link AuthorRepository} queries against the in-memory database; each test is rolled back. */
@Transactional
@SpringBootTest
class AuthorRepositoryIntegrationTest {
    @Autowired
    private AuthorRepository authorRepository;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private GenreRepository genreRepository;
    @Autowired
    private ReaderRepository readerRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LendingRepository lendingRepository;

    private Author joao;
    private Author joana;
    private Author maria;
    private Author pina;
    private Book encantos;
    private Book contos;
    private Book inspector;

    @BeforeEach
    void setUp() {
        joao = authorRepository.save(new Author("João Alberto", "O João Alberto nasceu em Chaves e foi pedreiro.", null));
        joana = authorRepository.save(new Author("Joana Vasconcelos", "A Joana Vasconcelos é artista plástica.", null));
        maria = authorRepository.save(new Author("Maria José", "A Maria José nasceu em Viseu e come laranjas.", null));
        pina = authorRepository.save(new Author("Manuel Antonio Pina", "Manuel António Pina foi um jornalista e escritor português.", null));
        final var fantasy = genreRepository.save(new Genre("Fantasia"));

        // João writes with Maria twice and with Joana once; Pina writes alone.
        encantos = bookRepository.save(new Book("9782826012092", "Encantos de contar", "Contos", fantasy, List.of(joao, maria), null));
        contos = bookRepository.save(new Book("9789722025065", "Contos de Chaves", "Contos", fantasy, List.of(joao, maria, joana), null));
        inspector = bookRepository.save(new Book("9789720706386", "O Inspetor Max", "Policial", fantasy, List.of(pina), null));
    }

    @Test
    void findByAuthorNumberReturnsTheAuthorOrEmpty() {
        assertThat(authorRepository.findByAuthorNumber(joao.getAuthorNumber())).contains(joao);
        assertThat(authorRepository.findByAuthorNumber(-1L)).isEmpty();
    }

    @Test
    void searchByNameStartsWithMatchesThePrefixOnly() {
        assertThat(authorRepository.searchByNameNameStartsWith("Jo")).containsExactlyInAnyOrder(joao, joana);
        assertThat(authorRepository.searchByNameNameStartsWith("José")).isEmpty();
    }

    @Test
    void searchByNameMatchesTheWholeName() {
        assertThat(authorRepository.searchByNameName("Maria José")).containsExactly(maria);
        assertThat(authorRepository.searchByNameName("Maria")).isEmpty();
    }

    @Test
    void coAuthorsAreTheOtherAuthorsOfTheSameBooksWithoutRepetitions() {
        assertThat(authorRepository.findCoAuthorsByAuthorNumber(joao.getAuthorNumber())).containsExactlyInAnyOrder(maria, joana);
        assertThat(authorRepository.findCoAuthorsByAuthorNumber(joana.getAuthorNumber())).containsExactlyInAnyOrder(joao, maria);
    }

    @Test
    void anAuthorWhoWritesAloneHasNoCoAuthors() {
        assertThat(authorRepository.findCoAuthorsByAuthorNumber(pina.getAuthorNumber())).isEmpty();
    }

    @Test
    void topAuthorsAreOrderedByTheirNumberOfLendings() {
        final var user = userRepository.save(Reader.newReader("manuel@gmail.com", "Manuelino123!", "Manuel Sarapinto das Coives"));
        final var reader = readerRepository.save(new ReaderDetails(1, user, "2000-01-01", "919191919", true, true, true, null, null));
        final var today = LocalDate.now();
        lendingRepository.save(new Lending(encantos, reader, 1, 14, 50, today));
        lendingRepository.save(new Lending(contos, reader, 2, 14, 50, today));
        lendingRepository.save(new Lending(inspector, reader, 3, 14, 50, today));

        final List<AuthorLendingView> top = authorRepository.findTopAuthorByLendings(PageRequest.of(0, 5)).getContent();

        // João and Maria are in both lent books, Joana and Pina in one each.
        assertThat(top).hasSize(4);
        assertThat(top.subList(0, 2)).extracting(AuthorLendingView::getAuthorName).containsExactlyInAnyOrder("João Alberto", "Maria José");
        assertThat(top.get(0).getLendingCount()).isEqualTo(2L);
        assertThat(top.get(2).getLendingCount()).isEqualTo(1L);
    }

    @Test
    void topAuthorsAreLimitedToTheRequestedPageSize() {
        final var user = userRepository.save(Reader.newReader("manuel@gmail.com", "Manuelino123!", "Manuel Sarapinto das Coives"));
        final var reader = readerRepository.save(new ReaderDetails(1, user, "2000-01-01", "919191919", true, true, true, null, null));
        lendingRepository.save(new Lending(contos, reader, 1, 14, 50, LocalDate.now()));

        assertThat(authorRepository.findTopAuthorByLendings(PageRequest.of(0, 2)).getContent()).hasSize(2);
    }
}
