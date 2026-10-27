package pt.psoft.g1.psoftg1.genremanagement.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;

/** SUT: GenreLendingsDTO string constructors; isolated formatting and numeric boundary tests. */
class GenreLendingsDTOTest {
    @ParameterizedTest
    @CsvSource({"0.0,0.0", "1.24,1.2", "1.26,1.3", "9.99,10.0"})
    void roundsAveragesToOneDecimal(double input, double expected) {
        // Arrange: average and expected rounded value supplied by the parameter source.
        // Act
        var result = new GenreLendingsDTO("Fiction", input);
        // Assert
        assertThat(result.getGenre()).isEqualTo("Fiction");
        assertThat(result.getValue().doubleValue()).isEqualTo(expected);
    }

    @Test
    void nullAverageBecomesZero() {
        // Arrange
        Double average = null;
        // Act
        var result = new GenreLendingsDTO("Fiction", average);
        // Assert
        assertThat(result.getValue().intValue()).isZero();
    }

    @Test
    void countsRetainLongPrecision() {
        // Arrange
        long count = 9_007_199_254_740_993L;
        // Act
        var result = new GenreLendingsDTO("Fiction", count);
        // Assert
        assertThat(result.getValue()).isEqualTo(count);
    }
}
