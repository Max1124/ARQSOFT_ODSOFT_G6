package pt.psoft.g1.psoftg1.testutils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
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
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Shared fixtures for tests that run the real controllers, services, repositories and database through MockMvc.
 * Each test runs in a transaction that is rolled back, so the database is left untouched.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class ApiIntegrationTestSupport {
    protected static final String PASSWORD = "Passw0rd!123";

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected ReaderRepository readerRepository;
    @Autowired
    protected BookRepository bookRepository;
    @Autowired
    protected AuthorRepository authorRepository;
    @Autowired
    protected GenreRepository genreRepository;
    @Autowired
    protected LendingRepository lendingRepository;

    private int nextReaderNumber = 1;
    private int nextIsbn = 1;
    private int nextSeq = 100;

    protected Genre newGenre(String name) {
        return genreRepository.save(new Genre(name));
    }

    protected Author newAuthor(String name) {
        return authorRepository.save(new Author(name, "Biografia de " + name + " para os testes.", null));
    }

    protected Librarian newLibrarian(String email) {
        return userRepository.save(Librarian.newLibrarian(email, PASSWORD, "Maria Roberta"));
    }

    protected ReaderDetails newReader(String email) {
        final Reader user = userRepository.save(Reader.newReader(email, PASSWORD, "Manuel Sarapinto das Coives"));
        return readerRepository.save(new ReaderDetails(nextReaderNumber++, user, "2000-01-01", "919191919", true, true, true, null, null));
    }

    protected Book newBook(String title, Genre genre, Author author) {
        return bookRepository.save(new Book(nextValidIsbn(), title, "Descrição de " + title, genre, List.of(author), null));
    }

    /** A different valid ISBN-13 on each call. */
    protected String nextValidIsbn() {
        final String first12 = String.format("978%09d", nextIsbn++);
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            final int digit = first12.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        return first12 + (10 - sum % 10) % 10;
    }

    /** Seeds a lending directly (history set-up, not the behaviour under test). Sequences stay clear of the ones the service generates. */
    protected Lending seedLending(Book book, ReaderDetails reader, LocalDate start, LocalDate returned, int periodDays) {
        return lendingRepository.save(Lending.newBootstrappingLending(book, reader, start.getYear(), nextSeq++, start, returned, periodDays, 200));
    }

    /** Authenticates a request as the given user, with the JWT subject format the application uses ("id,username"). */
    protected static RequestPostProcessor asUser(User user, String role) {
        return jwt().jwt(j -> j.subject(user.getId() + "," + user.getUsername()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
