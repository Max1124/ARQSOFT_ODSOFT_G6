package pt.psoft.g1.psoftg1.bootstrapping;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Starts the application with the "bootstrap" profile, as it runs in development, on its own in-memory database
 * so the seed data does not leak into the other tests.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:bootstrapper-it")
@ActiveProfiles("bootstrap")
class BootstrapperIntegrationTest {

    @Autowired
    private UserBootstrapper userBootstrapper;
    @Autowired
    private Bootstrapper bootstrapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthorRepository authorRepository;
    @Autowired
    private GenreRepository genreRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void startupSeedsUsersCatalogueAndLendings() {
        assertTrue(userRepository.findByUsername("manuel@gmail.com").isPresent());
        assertInstanceOf(Librarian.class, userRepository.findByUsername("maria@gmail.com").orElseThrow());
        assertFalse(authorRepository.searchByNameName("Manuel Antonio Pina").isEmpty());
        assertTrue(genreRepository.findByString("Fantasia").isPresent());
        assertTrue(count("ReaderDetails") > 0);
        assertTrue(count("Book") > 0);
        assertTrue(count("Lending") > 0);
        assertTrue(count("ForbiddenName") > 0);
    }

    @Test
    @Transactional
    void runningTheSeedersAgainDoesNotDuplicateData() {
        final List<Long> before = countAll();

        userBootstrapper.run();
        bootstrapper.run();

        assertEquals(before, countAll());
    }

    private List<Long> countAll() {
        return List.of(count("User"), count("ReaderDetails"), count("Author"), count("Genre"),
                count("Book"), count("Lending"), count("ForbiddenName"));
    }

    private long count(String entity) {
        return entityManager.createQuery("select count(e) from " + entity + " e", Long.class).getSingleResult();
    }
}
