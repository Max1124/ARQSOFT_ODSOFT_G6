package pt.psoft.g1.psoftg1.bibliographicmanagement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.*;

import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bibliographicmanagement.model.*;
import pt.psoft.g1.psoftg1.bibliographicmanagement.repositories.*;
import pt.psoft.g1.psoftg1.bibliographicmanagement.services.*;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;

import java.util.*;

class CatalogueMatcherTest {
    AuthorRepository authors;
    GenreRepository genres;
    AuthorCorrespondenceRepository confirmations;
    CatalogueMatcher sut;

    @BeforeEach
    void setup() {
        authors = mock(AuthorRepository.class);
        genres = mock(GenreRepository.class);
        confirmations = mock(AuthorCorrespondenceRepository.class);
        sut = new CatalogueMatcher(authors, genres, confirmations);
    }

    BibliographicData data(List<String> suggestions) {
        return new BibliographicData(
                "9780132350884",
                "Title",
                null,
                List.of("Alice"),
                suggestions,
                Map.of("authors", "google-books"));
    }

    @Test
    void nameMatchOnlySuggestsAuthorAndUniqueGenreMatchesIgnoringCase() {
        // Arrange
        var author = mock(Author.class);
        when(author.getName()).thenReturn("Alice");
        when(author.getAuthorNumber()).thenReturn(1L);
        var genre = mock(Genre.class);
        when(genre.getGenre()).thenReturn("Fiction");
        when(authors.findAll()).thenReturn(List.of(author));
        when(genres.findAll()).thenReturn(List.of(genre));
        // Act
        var result = sut.resolve(data(List.of(" fiction ")), RegistrationRequest.empty());
        // Assert
        assertEquals("Fiction", result.genre());
        assertTrue(result.authorIds().isEmpty());
        assertEquals(List.of(1L), result.authorSuggestions().get("Alice"));
        assertEquals(List.of("Confirm catalogue author: Alice"), result.needs());
        verify(authors, never()).save(any());
        verify(genres, never()).save(any());
    }

    @Test
    void punctuationVariantsSuggestAllCandidatesButNeverAutomaticallyAssociateThem() {
        // Arrange: names are not unique identities, so even two matching candidates stay unresolved.
        var first = mock(Author.class);
        var second = mock(Author.class);
        var unrelated = mock(Author.class);
        when(first.getName()).thenReturn("Robert C Martin");
        when(first.getAuthorNumber()).thenReturn(1552L);
        when(second.getName()).thenReturn("Robert  C. Martin");
        when(second.getAuthorNumber()).thenReturn(1553L);
        when(unrelated.getName()).thenReturn("Martin Fowler");
        when(unrelated.getAuthorNumber()).thenReturn(1554L);
        when(authors.findAll()).thenReturn(List.of(first, second, unrelated));
        var metadata = new BibliographicData("9780132350884", "Clean Code", null,
                List.of("Robert C. Martin"), List.of(), Map.of());
        // Act
        var result = sut.resolve(metadata, RegistrationRequest.empty());
        // Assert
        assertEquals(List.of(1552L, 1553L), result.authorSuggestions().get("Robert C. Martin"));
        assertTrue(result.authorIds().isEmpty());
        assertTrue(result.needs().contains("Confirm catalogue author: Robert C. Martin"));
        verify(authors, never()).save(any());
        verify(confirmations, never()).save(any());
    }

    @Test
    void confirmedCorrespondenceResolvesAuthorButMultipleGenresNeedSelection() {
        // Arrange
        var correspondence = mock(AuthorCorrespondence.class);
        when(correspondence.getAuthorId()).thenReturn(7L);
        when(confirmations.findById(anyString())).thenReturn(Optional.of(correspondence));
        when(authors.findByAuthorNumber(7L)).thenReturn(Optional.of(mock(Author.class)));
        var first = mock(Genre.class);
        when(first.getGenre()).thenReturn("Fiction");
        var second = mock(Genre.class);
        when(second.getGenre()).thenReturn("History");
        when(genres.findAll()).thenReturn(List.of(first, second));
        // Act
        var result = sut.resolve(data(List.of("Fiction", "History")), RegistrationRequest.empty());
        // Assert
        assertEquals(List.of(7L), result.authorIds());
        assertNull(result.genre());
        assertEquals(List.of("Select one catalogue genre"), result.needs());
    }

    @Test
    void explicitUnknownIdsAndGenreNeverSilentlyRegister() {
        // Arrange
        var request = new RegistrationRequest(null, null, "unknown", List.of(99L));
        // Act
        var result = sut.resolve(null, request);
        // Assert
        assertTrue(result.authorIds().isEmpty());
        assertNull(result.genre());
        assertTrue(result.needs().contains("Unknown author ID: 99"));
        assertTrue(result.needs().contains("Select an existing catalogue genre"));
    }

    @Test
    void explicitValidChoicesWorkWithoutProviderData() {
        // Arrange
        var genre = mock(Genre.class);
        when(genre.getGenre()).thenReturn("Fiction");
        when(genres.findByString("Fiction")).thenReturn(Optional.of(genre));
        when(authors.findByAuthorNumber(7L)).thenReturn(Optional.of(mock(Author.class)));
        // Act
        var result =
                sut.resolve(
                        null, new RegistrationRequest("Manual", null, "Fiction", List.of(7L, 7L)));
        // Assert
        assertEquals(List.of(7L), result.authorIds());
        assertEquals("Fiction", result.genre());
        assertTrue(result.needs().isEmpty());
    }

    @Test
    void nonexistentConfirmationTargetIsRejectedWithoutWrite() {
        // Arrange / Act / Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> sut.confirm("google-books", "9780132350884", "Alice", 99L));
        verify(confirmations, never()).save(any());
    }
}
