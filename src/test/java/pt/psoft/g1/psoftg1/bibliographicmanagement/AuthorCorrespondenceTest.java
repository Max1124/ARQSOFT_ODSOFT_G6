package pt.psoft.g1.psoftg1.bibliographicmanagement;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import pt.psoft.g1.psoftg1.bibliographicmanagement.model.AuthorCorrespondence;

/** Transparent-box: domain correspondence validity and scoped identity boundaries. */
class AuthorCorrespondenceTest {
    @Test
    void identityIsScopedAndOuterSpacesAreIgnored() {
        // Arrange / Act
        var entry = new AuthorCorrespondence("google-books", "9780132350884", " Alice ", 3L);
        // Assert
        assertEquals(3L, entry.getAuthorId());
        assertEquals(
                AuthorCorrespondence.key("g", "isbn", "Alice"),
                AuthorCorrespondence.key("g", "isbn", " Alice "));
        assertNotEquals(
                AuthorCorrespondence.key("g", "isbn", "Alice"),
                AuthorCorrespondence.key("g", "other", "Alice"));
        assertNotEquals(
                AuthorCorrespondence.key("g", "isbn", "Alice"),
                AuthorCorrespondence.key("o", "isbn", "Alice"));
    }

    @Test
    void invalidIdentityOrCatalogueIdRejected() {
        // Arrange / Act / Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> new AuthorCorrespondence("g", "isbn", "Alice", 0L));
        assertThrows(
                IllegalArgumentException.class,
                () -> new AuthorCorrespondence("g", "isbn", "Alice", null));
        assertThrows(
                IllegalArgumentException.class,
                () -> AuthorCorrespondence.key(" ", "isbn", "Alice"));
        assertThrows(
                IllegalArgumentException.class, () -> AuthorCorrespondence.key("g", null, "Alice"));
        assertThrows(
                IllegalArgumentException.class, () -> AuthorCorrespondence.key("g", "isbn", " "));
    }

    @Test
    void smallestPositiveCatalogueIdIsAccepted() {
        // Arrange / Act
        var entry = new AuthorCorrespondence("google-books", "9780132350884", "Alice", 1L);
        // Assert
        assertEquals(1L, entry.getAuthorId());
    }
}
