package pt.psoft.g1.psoftg1.genremanagement.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

/** SUT: Genre alone; isolated domain boundary tests, without persistence or validation infrastructure. */
class GenreBoundaryTest {
    @ParameterizedTest
    @ValueSource(ints = {1, 99, 100})
    void acceptsNamesUpToTheExactLimit(int length) {
        // Arrange
        String name = "a".repeat(length);
        // Act
        Genre genre = new Genre(name);
        // Assert
        assertThat(genre.getGenre()).isEqualTo(name);
        assertThat(genre.toString()).isEqualTo(name);
    }

    @ParameterizedTest
    @ValueSource(ints = {101, 102})
    void rejectsNamesImmediatelyAboveTheLimit(int length) {
        // Arrange
        String name = "a".repeat(length);
        // Act / Assert
        assertThatIllegalArgumentException().isThrownBy(() -> new Genre(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "\t", "\r\n", "\u2003"})
    void rejectsWhitespaceOnlyNames(String name) {
        // Arrange: whitespace-only input supplied by the parameter source.
        // Act / Assert
        assertThatIllegalArgumentException().isThrownBy(() -> new Genre(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {" Fiction ", "Ficção científica", "A\tB"})
    void preservesNonBlankNamesWithoutTrimming(String name) {
        // Arrange: nonblank input supplied by the parameter source.
        // Act
        Genre genre = new Genre(name);
        // Assert
        assertThat(genre.getGenre()).isEqualTo(name);
    }
}
