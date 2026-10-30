package pt.psoft.g1.psoftg1.bibliographicmanagement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import pt.psoft.g1.psoftg1.bibliographicmanagement.infrastructure.*;
import pt.psoft.g1.psoftg1.bibliographicmanagement.services.*;

import java.io.IOException;
import java.net.URI;

class ProviderAdapterTest {
    @Test
    void openLibraryIoAndInterruptionFailuresNeverCallMapper() throws Exception {
        // Arrange
        var transport = mock(BibliographicTransport.class);
        var mapper = mock(OpenLibraryMapper.class);
        var adapter = new OpenLibraryAdapter(
                transport, mapper, "http://localhost/books"
        );

        // First call throws IOException, second throws InterruptedException
        doThrow(new IOException("offline"))
                .doThrow(new InterruptedException("cancelled"))
                .when(transport).execute(any());

        // Act / Assert - IOException
        var ioResult = adapter.lookupByIsbn("9780132350884");

        assertEquals(SourceOutcome.Status.FAILURE, ioResult.status());

        // Act / Assert - InterruptedException
        try {
            var interruptedResult = adapter.lookupByIsbn("9780132350884");

            assertEquals(SourceOutcome.Status.FAILURE, interruptedResult.status());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }

        verify(transport, times(2)).execute(any());
        verifyNoInteractions(mapper);
    }
    @Test
    void googleSendsEncodedKeyAndDoesNotExposeItInFailure() throws Exception {
        // Arrange: fake credential only; never use a real key in tests.
        var transport = mock(BibliographicTransport.class);
        var mapper = mock(GoogleBooksMapper.class);
        var uri = URI.create("http://localhost/books?q=isbn:9780132350884&maxResults=40&key=fake%2Bkey%26value");
        when(transport.execute(uri))
                .thenReturn(new BibliographicTransport.TransportResponse(429, "fake+key&value"));
        var adapter = new GoogleBooksAdapter(transport, mapper, "http://localhost/books", "fake+key&value");
        // Act
        var result = adapter.lookupByIsbn("9780132350884");
        // Assert
        verify(transport).execute(uri);
        assertEquals(SourceOutcome.Status.FAILURE, result.status());
        assertEquals("HTTP 429", result.reason());
        verifyNoInteractions(mapper);
    }

    @Test
    void googleUsesInjectedTransportAndMapper() throws Exception {
        // Arrange
        var transport = mock(BibliographicTransport.class);
        var mapper = mock(GoogleBooksMapper.class);
        var uri = URI.create("http://localhost/books?q=isbn:9780132350884&maxResults=40");
        when(transport.execute(uri))
                .thenReturn(new BibliographicTransport.TransportResponse(200, "body"));
        var expected = SourceOutcome.notFound("google-books");
        when(mapper.map("9780132350884", "body")).thenReturn(expected);
        // Act
        var result =
                new GoogleBooksAdapter(transport, mapper, "http://localhost/books")
                        .lookupByIsbn("9780132350884");
        // Assert
        assertSame(expected, result);
        verify(transport).execute(uri);
        verify(mapper).map("9780132350884", "body");
    }

    @Test
    void serverErrorDoesNotReachMapper() throws Exception {
        // Arrange
        var transport = mock(BibliographicTransport.class);
        var mapper = mock(OpenLibraryMapper.class);
        when(transport.execute(any()))
                .thenReturn(new BibliographicTransport.TransportResponse(503, "unavailable"));
        // Act
        var result =
                new OpenLibraryAdapter(transport, mapper, "http://localhost/books")
                        .lookupByIsbn("9780132350884");
        // Assert
        assertEquals(SourceOutcome.Status.FAILURE, result.status());
        verifyNoInteractions(mapper);
    }

    @Test
    void ioFailureBecomesFailureOutcome() throws Exception {
        // Arrange
        var transport = mock(BibliographicTransport.class);
        var mapper = mock(GoogleBooksMapper.class);
        when(transport.execute(any())).thenThrow(new IOException("offline"));
        // Act
        var result =
                new GoogleBooksAdapter(transport, mapper, "http://localhost/books")
                        .lookupByIsbn("9780132350884");
        // Assert
        assertEquals(SourceOutcome.Status.FAILURE, result.status());
        verifyNoInteractions(mapper);
    }

    @Test
    void openLibraryInvalidPayloadBecomesFailure() throws Exception {
        // Arrange
        var transport = mock(BibliographicTransport.class);
        var mapper = mock(OpenLibraryMapper.class);
        when(transport.execute(any()))
                .thenReturn(new BibliographicTransport.TransportResponse(200, "broken"));
        when(mapper.map("9780132350884", "broken"))
                .thenThrow(new IllegalArgumentException("invalid"));
        // Act
        var result =
                new OpenLibraryAdapter(transport, mapper, "http://localhost/books")
                        .lookupByIsbn("9780132350884");
        // Assert
        assertEquals(SourceOutcome.Status.FAILURE, result.status());
        assertNull(result.data());
    }

    @Test
    void cancellationPreservesInterruptStatus() throws Exception {
        // Arrange
        var transport = mock(BibliographicTransport.class);
        var mapper = mock(GoogleBooksMapper.class);
        when(transport.execute(any())).thenThrow(new InterruptedException("cancelled"));
        try {
            // Act
            var result =
                    new GoogleBooksAdapter(transport, mapper, "http://localhost/books")
                            .lookupByIsbn("9780132350884");
            // Assert
            assertEquals(SourceOutcome.Status.FAILURE, result.status());
            assertTrue(Thread.currentThread().isInterrupted());
            verifyNoInteractions(mapper);
        } finally {
            Thread.interrupted();
        }
    }
}
