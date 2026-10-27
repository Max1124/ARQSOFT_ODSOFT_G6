package pt.psoft.g1.psoftg1.readermanagement.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import pt.psoft.g1.psoftg1.external.service.ApiNinjasService;
import pt.psoft.g1.psoftg1.lendingmanagement.api.LendingViewMapper;
import pt.psoft.g1.psoftg1.lendingmanagement.services.LendingService;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderService;
import pt.psoft.g1.psoftg1.readermanagement.services.UpdateReaderRequest;
import pt.psoft.g1.psoftg1.shared.services.ConcurrencyService;
import pt.psoft.g1.psoftg1.shared.services.FileStorageService;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.services.UserService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Class/unit SUT: ReaderController, invoked directly with mocked services, mapper and entities.
 * No MockMvc, Spring context, HTTP binding or annotation/security-filter assertions. */
class ReaderControllerUnitTest {
    private ReaderService readers;
    private UserService users;
    private ReaderViewMapper mapper;
    private ConcurrencyService concurrency;
    private FileStorageService files;
    private ReaderController sut;

    @BeforeEach
    void setUp() {
        readers = mock(ReaderService.class);
        users = mock(UserService.class);
        mapper = mock(ReaderViewMapper.class);
        concurrency = mock(ConcurrencyService.class);
        files = mock(FileStorageService.class);
        sut = new ReaderController(readers, users, mapper, mock(LendingService.class),
                mock(LendingViewMapper.class), concurrency, files, mock(ApiNinjasService.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "null")
    void missingConditionalHeaderRejectsPatchBeforeSideEffects(String header) {
        // Arrange
        WebRequest web = mock(WebRequest.class);
        when(web.getHeader(ConcurrencyService.IF_MATCH)).thenReturn(header);
        // Act
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> sut.updateReader(new UpdateReaderRequest(), mock(Authentication.class), web));
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(readers, users, mapper, concurrency, files);
    }

    @Test
    void patchUsesAuthenticatedIdAndParsedVersionAndReturnsMappedBodyAndEtag() {
        // Arrange
        WebRequest web = mock(WebRequest.class);
        Authentication auth = mock(Authentication.class);
        Reader user = mock(Reader.class);
        ReaderDetails details = mock(ReaderDetails.class);
        ReaderView view = mock(ReaderView.class);
        UpdateReaderRequest request = new UpdateReaderRequest();
        when(web.getHeader(ConcurrencyService.IF_MATCH)).thenReturn("\"7\"");
        when(concurrency.getVersionFromIfMatchHeader("\"7\"")).thenReturn(7L);
        when(users.getAuthenticatedUser(auth)).thenReturn(user);
        when(user.getId()).thenReturn(42L);
        when(readers.update(42L, request, 7L, null)).thenReturn(details);
        when(details.getVersion()).thenReturn(8L);
        when(mapper.toReaderView(details)).thenReturn(view);
        // Act
        var response = sut.updateReader(request, auth, web);
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("\"8\"", response.getHeaders().getETag());
        assertSame(view, response.getBody());
        verify(readers).update(42L, request, 7L, null);
    }

    @Test
    void anotherReadersPhotoIsDeniedBeforeFileAccess() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        Reader user = mock(Reader.class);
        ReaderDetails ownDetails = mock(ReaderDetails.class);
        when(users.getAuthenticatedUser(auth)).thenReturn(user);
        when(user.getUsername()).thenReturn("ana@example.org");
        when(readers.findByUsername("ana@example.org")).thenReturn(Optional.of(ownDetails));
        when(ownDetails.getReaderNumber()).thenReturn("2024/1");
        // Act: capture the operation for the exception assertion.
        org.junit.jupiter.api.function.Executable action = () -> sut.getSpecificReaderPhoto(2024, 2, auth);
        // Assert
        assertThrows(AccessDeniedException.class, action);
        verify(readers, never()).findByReaderNumber(anyString());
        verifyNoInteractions(files);
    }

    @Test
    void ownPhotoReturnsNotFoundWhenReaderHasNoPhoto() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        Reader user = mock(Reader.class);
        ReaderDetails details = mock(ReaderDetails.class);
        when(users.getAuthenticatedUser(auth)).thenReturn(user);
        when(user.getUsername()).thenReturn("ana@example.org");
        when(readers.findByUsername("ana@example.org")).thenReturn(Optional.of(details));
        // Act
        var response = sut.getReaderOwnPhoto(auth);
        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verifyNoInteractions(files);
    }
}
