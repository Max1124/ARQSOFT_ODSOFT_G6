package pt.psoft.g1.psoftg1.bibliographicmanagement;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import pt.psoft.g1.psoftg1.bibliographicmanagement.infrastructure.*;
import pt.psoft.g1.psoftg1.bibliographicmanagement.services.*;

import java.util.*;

/** Mapper + standard Jackson parsing; no HTTP or persistence. */
class ProviderMappingTest {
    static final String ISBN = "9780132350884";

    @Test
    void googleMapsExactIsbnAndPartialFields() throws Exception {
        // Arrange
        String body =
                """
{"totalItems":2,"items":[{"volumeInfo":{"title":"Wrong","industryIdentifiers":[{"identifier":"0000000000000"}]}},
{"volumeInfo":{"title":"Clean Code","industryIdentifiers":[{"identifier":"9780132350884"}],"authors":["Robert Martin"],"categories":["Computers"]}}]}
""";
        // Act
        var result = new GoogleBooksMapper(new ObjectMapper()).map(ISBN, body);
        // Assert
        assertEquals(SourceOutcome.Status.FOUND, result.status());
        assertEquals("Clean Code", result.data().title());
        assertNull(result.data().description());
        assertEquals(List.of("Robert Martin"), result.data().authors());
    }

    @Test
    void openLibraryMapsNamedObjects() throws Exception {
        // Arrange
        String body =
                """
{"ISBN:9780132350884":{"title":"Clean Code","authors":[{"name":"Robert Martin"}],"subjects":[{"name":"Computers"}]}}
""";
        // Act
        var result = new OpenLibraryMapper(new ObjectMapper()).map(ISBN, body);
        // Assert
        assertEquals(List.of("Computers"), result.data().genres());
        assertEquals(List.of("Robert Martin"), result.data().authors());
        assertNull(result.data().description());
    }

    @Test
    void providersDistinguishEmptyResults() throws Exception {
        // Arrange / Act / Assert
        assertEquals(
                SourceOutcome.Status.NOT_FOUND,
                new GoogleBooksMapper(new ObjectMapper()).map(ISBN, "{\"totalItems\":0}").status());
        assertEquals(
                SourceOutcome.Status.NOT_FOUND,
                new OpenLibraryMapper(new ObjectMapper()).map(ISBN, "{}").status());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "[]",
                "null",
                "not json",
                "{\"ISBN:9780132350884\":false}",
                "{\"ISBN:9780132350884\":{\"authors\":false}}"
            })
    void malformedOpenLibraryResponseRejected(String body) {
        // Arrange
        var mapper = new OpenLibraryMapper(new ObjectMapper());
        // Act / Assert
        assertThrows(Exception.class, () -> mapper.map(ISBN, body));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "[]",
                "null",
                "{}",
                "{\"totalItems\":1}",
                "{\"totalItems\":1,\"items\":[{}]}"
            })
    void malformedGoogleResponseIsNotReportedAsNoMatch(String body) {
        // Arrange
        var mapper = new GoogleBooksMapper(new ObjectMapper());
        // Act / Assert
        assertThrows(Exception.class, () -> mapper.map(ISBN, body));
    }

    @Test
    void googleDoesNotUseMetadataForAnotherIsbn() throws Exception {
        // Arrange
        String body =
                "{\"totalItems\":1,\"items\":[{\"volumeInfo\":{\"title\":\"Other"
                    + " edition\",\"industryIdentifiers\":[{\"identifier\":\"0000000000000\"}]}}]}";
        // Act
        var result = new GoogleBooksMapper(new ObjectMapper()).map(ISBN, body);
        // Assert
        assertEquals(SourceOutcome.Status.NOT_FOUND, result.status());
        assertNull(result.data());
    }
}
