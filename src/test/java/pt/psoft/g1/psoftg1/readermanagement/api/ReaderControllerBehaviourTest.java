package pt.psoft.g1.psoftg1.readermanagement.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import java.time.LocalDate;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import jakarta.validation.ValidationException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.external.service.ApiNinjasService;
import pt.psoft.g1.psoftg1.lendingmanagement.api.*;
import pt.psoft.g1.psoftg1.lendingmanagement.services.LendingService;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.readermanagement.model.*;
import pt.psoft.g1.psoftg1.readermanagement.services.*;
import pt.psoft.g1.psoftg1.shared.model.Photo;
import pt.psoft.g1.psoftg1.shared.services.*;
import pt.psoft.g1.psoftg1.usermanagement.model.*;
import pt.psoft.g1.psoftg1.usermanagement.services.UserService;

/** SUT: controller only. Behavioural services, mappers and domain objects are mocked. */
class ReaderControllerBehaviourTest {
    ReaderService readers; UserService users; ReaderViewMapper mapper;
    LendingService lendings; LendingViewMapper lendingMapper; FileStorageService files;
    ApiNinjasService ninjas; Authentication auth; ReaderController sut;
    @BeforeEach void setup() {
        readers=mock(ReaderService.class); users=mock(UserService.class); mapper=mock(ReaderViewMapper.class);
        lendings=mock(LendingService.class); lendingMapper=mock(LendingViewMapper.class);
        files=mock(FileStorageService.class); ninjas=mock(ApiNinjasService.class); auth=mock(Authentication.class);
        sut=new ReaderController(readers,users,mapper,lendings,lendingMapper,mock(ConcurrencyService.class),files,ninjas);
    }
    private ReaderDetails ownReader() {
        Reader user=mock(Reader.class); ReaderDetails details=mock(ReaderDetails.class);
        when(users.getAuthenticatedUser(auth)).thenReturn(user);
        when(user.getUsername()).thenReturn("reader@example.org");
        when(readers.findByUsername("reader@example.org")).thenReturn(Optional.of(details));
        return details;
    }
    private void photo(ReaderDetails details) {
        Photo photo=mock(Photo.class);
        when(details.getPhoto()).thenReturn(photo); when(photo.getPhotoFile()).thenReturn("portrait.png");
    }
    @ParameterizedTest @ValueSource(strings={"png","jpg"})
    void ownPhotoReturnsExactBytesAndMediaType(String extension) {
        // Arrange
        var details=ownReader(); photo(details); byte[] bytes={1,2,3};
        when(files.getFile("portrait.png")).thenReturn(bytes);
        when(files.getExtension("portrait.png")).thenReturn(Optional.of(extension));
        // Act
        var result=sut.getReaderOwnPhoto(auth);
        // Assert
        assertArrayEquals(bytes,result.getBody());
        assertEquals(extension.equals("png")?MediaType.IMAGE_PNG:MediaType.IMAGE_JPEG,result.getHeaders().getContentType());
    }
    @Test void missingFileAndUnknownExtensionAreHandledSeparately() {
        // Arrange
        var details=ownReader(); photo(details);
        // Act / Assert: missing file does not attempt to infer its media type.
        assertNull(sut.getReaderOwnPhoto(auth).getBody());
        verify(files,never()).getExtension(anyString());
        when(files.getFile("portrait.png")).thenReturn(new byte[]{1});
        assertThrows(ValidationException.class,()->sut.getReaderOwnPhoto(auth));
    }
    @Test void deletingOwnPhotoUsesReaderIdentityVersionAndFile() {
        // Arrange
        var details=ownReader(); photo(details);
        when(details.getReaderNumber()).thenReturn("2026/7"); when(details.getVersion()).thenReturn(4L);
        // Act
        assertEquals(HttpStatus.OK,sut.deleteReaderPhoto(auth).getStatusCode());
        // Assert
        verify(files).deleteFile("portrait.png"); verify(readers).removeReaderPhoto("2026/7",4L);
    }
    @Test void missingReaderOrPhotoCannotBeDeleted() {
        // Arrange
        var details=ownReader();
        // Act / Assert
        assertThrows(NotFoundException.class,()->sut.deleteReaderPhoto(auth));
        when(readers.findByUsername("reader@example.org")).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class,()->sut.deleteReaderPhoto(auth));
        assertThrows(AccessDeniedException.class,()->sut.getReaderOwnPhoto(auth));
        verifyNoInteractions(files);
        verify(readers,never()).removeReaderPhoto(anyString(),anyLong());
    }
    @Test void librarianCanReadSpecificPhotoButMissingReaderIsRejected() {
        // Arrange
        when(users.getAuthenticatedUser(auth)).thenReturn(mock(Librarian.class));
        var details=mock(ReaderDetails.class); photo(details);
        when(readers.findByReaderNumber("2026/7")).thenReturn(Optional.of(details));
        when(files.getFile("portrait.png")).thenReturn(new byte[]{9});
        when(files.getExtension("portrait.png")).thenReturn(Optional.of("png"));
        // Act / Assert
        assertArrayEquals(new byte[]{9},sut.getSpecificReaderPhoto(2026,7,auth).getBody());
        assertThrows(NotFoundException.class,()->sut.getSpecificReaderPhoto(2026,8,auth));
    }
    @Test void nameSearchSkipsUsersWithoutReaderDetails() {
        // Arrange
        var first=mock(User.class); var other=mock(User.class); var details=mock(ReaderDetails.class);
        when(first.getUsername()).thenReturn("reader"); when(other.getUsername()).thenReturn("librarian");
        when(users.findByNameLike("Ana")).thenReturn(List.of(first,other));
        when(readers.findByUsername("reader")).thenReturn(Optional.of(details));
        var views=List.of(mock(ReaderView.class)); when(mapper.toReaderView(List.of(details))).thenReturn(views);
        // Act / Assert
        assertEquals(views,sut.findByReaderName("Ana").getItems());
        assertThrows(NotFoundException.class,()->sut.findByReaderName("Absent"));
        verify(mapper).toReaderView(List.of(details));
    }
    @Test void quoteUsesBirthYearMonthAndPreservesVersion() {
        // Arrange
        var details=mock(ReaderDetails.class); var date=mock(BirthDate.class); var view=new ReaderQuoteView();
        when(readers.findByReaderNumber("2026/7")).thenReturn(Optional.of(details));
        when(details.getBirthDate()).thenReturn(date); when(date.getBirthDate()).thenReturn(LocalDate.of(1990,3,4));
        when(details.getVersion()).thenReturn(5L); when(mapper.toReaderQuoteView(details)).thenReturn(view);
        when(ninjas.getRandomEventFromYearMonth(1990,3)).thenReturn("Controlled event");
        // Act
        var result=sut.findByReaderNumber(2026,7);
        // Assert
        assertEquals("Controlled event",result.getBody().getQuote()); assertEquals("\"5\"",result.getHeaders().getETag());
        verify(ninjas).getRandomEventFromYearMonth(1990,3);
        assertThrows(NotFoundException.class,()->sut.findByReaderNumber(2026,8));
    }
    @Test void readerCanSeeOwnLendingsButNotOtherReaders() {
        // Arrange
        var own=ownReader(); when(own.getReaderNumber()).thenReturn("2026/7");
        when(readers.findByReaderNumber("2026/7")).thenReturn(Optional.of(own));
        var other=mock(ReaderDetails.class); when(other.getReaderNumber()).thenReturn("2026/8");
        when(readers.findByReaderNumber("2026/8")).thenReturn(Optional.of(other));
        var rows=List.of(mock(Lending.class)); var views=List.of(mock(LendingView.class));
        when(lendings.listByReaderNumberAndIsbn("2026/7","isbn",Optional.of(false))).thenReturn(rows);
        when(lendingMapper.toLendingView(rows)).thenReturn(views);
        // Act / Assert
        assertEquals(views,sut.getReaderLendings(auth,2026,7,"isbn",Optional.of(false)));
        assertThrows(AccessDeniedException.class,()->sut.getReaderLendings(auth,2026,8,"isbn",Optional.empty()));
        verify(lendings,never()).listByReaderNumberAndIsbn(eq("2026/8"),anyString(),any());
        assertThrows(NotFoundException.class,()->sut.getReaderLendings(auth,2026,7,"absent",Optional.empty()));
    }
    @Test void genreRankingReturnsMappedCountsAndRejectsEmptyResult() {
        // Arrange
        LocalDate start=LocalDate.of(2026,1,1), end=start.plusDays(1);
        var rows=List.of(mock(ReaderBookCountDTO.class)); var views=List.of(mock(ReaderCountView.class));
        when(readers.findTopByGenre("History",start,end)).thenReturn(rows);
        when(mapper.toReaderCountViewList(rows)).thenReturn(views);
        // Act / Assert
        assertEquals(views,sut.getTop5ReaderByGenre("History",start,end).getItems());
        assertThrows(NotFoundException.class,()->sut.getTop5ReaderByGenre("Missing",start,end));
    }
}
