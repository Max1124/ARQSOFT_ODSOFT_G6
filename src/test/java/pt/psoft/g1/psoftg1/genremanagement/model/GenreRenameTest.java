package pt.psoft.g1.psoftg1.genremanagement.model;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.Arrays;

/** Domain SUT: Genre. Cases derive from rename validation boundaries and state preservation. */
class GenreRenameTest {
    @Test void acceptsBothLengthBoundaries() {
        // Arrange
        var genre=new Genre("Old");
        // Act / Assert
        genre.rename("A"); assertEquals("A",genre.getGenre());
        genre.rename("A".repeat(100)); assertEquals("A".repeat(100),genre.getGenre());
    }
    @Test void rejectedRenamePreservesPreviousValue() {
        // Arrange
        var genre=new Genre("Computers");
        // Act / Assert
        for(String invalid : Arrays.asList(null,"","   ","A".repeat(101))) {
            assertThrows(IllegalArgumentException.class,()->genre.rename(invalid));
            assertEquals("Computers",genre.getGenre());
        }
    }
}
