package pt.psoft.g1.psoftg1.bibliographicmanagement;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import pt.psoft.g1.psoftg1.bibliographicmanagement.infrastructure.OpenLibraryMapper;

/** Mapper + JSON helper collaboration; response variants are controlled fixtures. */
class OpenLibraryDescriptionTest {
    @Test void acceptsTextAndValueObjectDescriptionsButRejectsInvalidTypes() throws Exception {
        // Arrange
        var mapper=new OpenLibraryMapper(new ObjectMapper());
        String prefix="{\"ISBN:9780132350884\":{\"description\":";
        // Act / Assert
        assertEquals("Text",mapper.map("9780132350884",prefix+"\"Text\"}}").data().description());
        assertEquals("Text",mapper.map("9780132350884",prefix+"{\"value\":\"Text\"}}}").data().description());
        assertThrows(IllegalArgumentException.class,()->mapper.map("9780132350884",prefix+"42}}"));
    }
}
