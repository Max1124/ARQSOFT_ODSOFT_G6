package pt.psoft.g1.psoftg1.genremanagement.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.*;
import java.util.*;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.exceptions.*;

/** Service + Genre collaboration: create/update intentionally execute real domain validation. */
class GenreCrudServiceTest {
    GenreRepository repo; GenreServiceImpl sut;
    @BeforeEach void setup() { repo=mock(GenreRepository.class); sut=new GenreServiceImpl(repo); }
    @Test void createTrimsNameAndReturnsSavedEntity() {
        // Arrange
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        // Act
        var result=sut.create("  Computers  ");
        // Assert
        assertEquals("Computers",result.getGenre()); verify(repo).save(result);
    }
    @Test void invalidCreateDoesNotWriteAndDuplicateIgnoresCase() {
        // Arrange
        when(repo.findAll()).thenReturn(List.of(new Genre("Computers")));
        // Act / Assert
        for(String invalid : Arrays.asList(null," ","x".repeat(101))) {
            assertThrows(IllegalArgumentException.class,()->sut.create(invalid));
        }
        assertThrows(ConflictException.class,()->sut.create(" computers "));
        verify(repo,never()).save(any());
    }
    @Test void renameKeepsIdentityAndRejectsInvalidNameWithoutMutation() {
        // Arrange
        Genre existing=new Genre("Computers");
        when(repo.findByString("Computers")).thenReturn(Optional.of(existing));
        when(repo.findAll()).thenReturn(List.of(existing));
        when(repo.save(existing)).thenReturn(existing);
        // Act / Assert
        assertThrows(IllegalArgumentException.class,()->sut.update("Computers",null));
        assertEquals("Computers",existing.getGenre());
        assertSame(existing,sut.update("Computers"," Computing "));
        assertEquals("Computing",existing.getGenre());
    }
    @Test void referencedGenreNeverReachesDelete() {
        // Arrange: class isolation here; no real domain behaviour is required.
        Genre existing=mock(Genre.class);
        when(repo.findByString("Computers")).thenReturn(Optional.of(existing));
        when(repo.isUsedByBooks(existing)).thenReturn(true);
        // Act / Assert
        assertThrows(ConflictException.class,()->sut.delete("Computers"));
        verify(repo,never()).delete(any());
        verify(repo,never()).isUsedByReaders(any());
    }
}
