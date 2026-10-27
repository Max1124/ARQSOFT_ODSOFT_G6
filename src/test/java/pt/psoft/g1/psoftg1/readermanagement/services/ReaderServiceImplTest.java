package pt.psoft.g1.psoftg1.readermanagement.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.shared.repositories.ForbiddenNameRepository;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Class/unit SUT: ReaderServiceImpl. Repositories, mapper and domain entities are mocks.
 * Requests and paging objects are data carriers; no Spring, persistence or real domain validation. */
class ReaderServiceImplTest {
    private ReaderRepository readers;
    private UserRepository users;
    private ReaderMapper mapper;
    private GenreRepository genres;
    private ForbiddenNameRepository forbidden;
    private PhotoRepository photos;
    private ReaderServiceImpl sut;

    @BeforeEach
    void setUp() {
        readers = mock(ReaderRepository.class);
        users = mock(UserRepository.class);
        mapper = mock(ReaderMapper.class);
        genres = mock(GenreRepository.class);
        forbidden = mock(ForbiddenNameRepository.class);
        photos = mock(PhotoRepository.class);
        sut = new ReaderServiceImpl(readers, users, mapper, genres, forbidden, photos);
    }

    private CreateReaderRequest request() {
        CreateReaderRequest request = new CreateReaderRequest();
        request.setUsername("ana@example.org");
        request.setFullName("Ana Silva");
        return request;
    }

    @Test
    void duplicateUsernameStopsCreationBeforeMappingOrSaving() {
        // Arrange
        CreateReaderRequest request = request();
        when(users.findByUsername(request.getUsername())).thenReturn(Optional.of(mock(Reader.class)));
        // Act: capture the operation for the exception assertion.
        org.junit.jupiter.api.function.Executable action = () -> sut.create(request, null);
        // Assert
        assertThrows(ConflictException.class, action);
        verify(users, never()).save(any());
        verifyNoInteractions(readers, mapper, genres, forbidden, photos);
    }

    @Test
    void creationResolvesInterestsAndUsesNextCurrentYearSequence() {
        // Arrange
        CreateReaderRequest request = request();
        request.setInterestList(List.of("History"));
        Genre genre = mock(Genre.class);
        Reader reader = mock(Reader.class);
        ReaderDetails details = mock(ReaderDetails.class);
        when(genres.findByString("History")).thenReturn(Optional.of(genre));
        when(readers.getCountFromCurrentYear()).thenReturn(12);
        when(mapper.createReader(request)).thenReturn(reader);
        when(mapper.createReaderDetails(13, reader, request, null, List.of(genre))).thenReturn(details);
        when(readers.save(details)).thenReturn(details);
        // Act
        ReaderDetails result = sut.create(request, null);
        // Assert
        assertSame(details, result);
        verify(forbidden).findByForbiddenNameIsContained("Ana");
        verify(forbidden).findByForbiddenNameIsContained("Silva");
        var order = inOrder(users, readers);
        order.verify(users).save(reader);
        order.verify(readers).save(details);
    }

    @Test
    void unknownInterestPreventsCreation() {
        // Arrange
        CreateReaderRequest request = request();
        request.setInterestList(List.of("Unknown"));
        // Act: capture the operation for the exception assertion.
        org.junit.jupiter.api.function.Executable action = () -> sut.create(request, null);
        // Assert
        assertThrows(NotFoundException.class, action);
        verify(genres).findByString("Unknown");
        verifyNoInteractions(mapper, readers);
        verify(users, never()).save(any());
    }

    @Test
    void missingReaderPreventsUpdate() {
        // Arrange
        UpdateReaderRequest request = new UpdateReaderRequest();
        // Act: capture the operation for the exception assertion.
        org.junit.jupiter.api.function.Executable action = () -> sut.update(42L, request, 3L, null);
        // Assert
        assertThrows(NotFoundException.class, action);
        verify(readers, never()).save(any());
        verifyNoInteractions(users, genres);
    }

    @Test
    void stalePatchDoesNotPersistEitherEntity() {
        // Arrange
        ReaderDetails details = mock(ReaderDetails.class);
        UpdateReaderRequest request = new UpdateReaderRequest();
        when(readers.findByUserId(42L)).thenReturn(Optional.of(details));
        doThrow(new ConflictException("stale")).when(details).applyPatch(3L, request, null, null);
        // Act: capture the operation for the exception assertion.
        org.junit.jupiter.api.function.Executable action = () -> sut.update(42L, request, 3L, null);
        // Assert
        assertThrows(ConflictException.class, action);
        verify(readers, never()).save(any());
        verifyNoInteractions(users);
    }

    @Test
    void updatePassesResolvedInterestsAndVersionBeforeSaving() {
        // Arrange
        ReaderDetails details = mock(ReaderDetails.class);
        Reader reader = mock(Reader.class);
        Genre genre = mock(Genre.class);
        UpdateReaderRequest request = new UpdateReaderRequest();
        request.setInterestList(List.of("History"));
        when(readers.findByUserId(42L)).thenReturn(Optional.of(details));
        when(genres.findByString("History")).thenReturn(Optional.of(genre));
        when(details.getReader()).thenReturn(reader);
        when(readers.save(details)).thenReturn(details);
        // Act
        ReaderDetails result = sut.update(42L, request, 3L, null);
        // Assert
        assertSame(details, result);
        var order = inOrder(details, users, readers);
        order.verify(details).applyPatch(3L, request, null, List.of(genre));
        order.verify(users).save(reader);
        order.verify(readers).save(details);
    }

    @Test
    void nonPositiveTopSizeNeverQueriesRepository() {
        // Arrange: setUp supplies the service and untouched repository mocks.
        // Act: capture the operation for the exception assertion.
        org.junit.jupiter.api.function.Executable action = () -> sut.findTopReaders(0);
        org.junit.jupiter.api.function.Executable negativeSize = () -> sut.findTopReaders(-1);
        // Assert
        assertThrows(IllegalArgumentException.class, action);
        assertThrows(IllegalArgumentException.class, negativeSize);
        verifyNoInteractions(readers);
    }

    @Test
    void smallestTopSizeUsesFirstPageAndReturnsItsContent() {
        // Arrange
        ReaderDetails reader = mock(ReaderDetails.class);
        when(readers.findTopReaders(PageRequest.of(0, 1))).thenReturn(new PageImpl<>(List.of(reader)));
        // Act
        List<ReaderDetails> result = sut.findTopReaders(1);
        // Assert
        assertEquals(List.of(reader), result);
        verify(readers).findTopReaders(PageRequest.of(0, 1));
    }

    @Test
    void reversedDateRangeFailsBeforeQuerying() {
        // Arrange
        LocalDate start = LocalDate.of(2024, 2, 2);
        // Act: capture the operation for the exception assertion.
        org.junit.jupiter.api.function.Executable action = () -> sut.findTopByGenre("History", start, start.minusDays(1));
        // Assert
        assertThrows(IllegalArgumentException.class, action);
        verifyNoInteractions(readers);
    }

    @Test
    void equalDateBoundariesAreAllowedAndLimitResultsToFive() {
        // Arrange
        LocalDate date = LocalDate.of(2024, 2, 2);
        ReaderBookCountDTO row = mock(ReaderBookCountDTO.class);
        when(readers.findTopByGenre(PageRequest.of(0, 5), "History", date, date))
                .thenReturn(new PageImpl<>(List.of(row)));
        // Act
        var result = sut.findTopByGenre("History", date, date);
        // Assert
        assertEquals(List.of(row), result);
    }
}
