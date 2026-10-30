package pt.psoft.g1.psoftg1.readermanagement.model;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.readermanagement.services.UpdateReaderRequest;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Domain collaboration SUT: ReaderDetails with real ReaderNumber, BirthDate and PhoneNumber.
 * The public constructor prepares a valid aggregate; Reader and request are mocks.
 * Reflection supplies only the JPA-managed version. Real value-object behavior makes this
 * a multi-class collaboration, not an isolated ReaderDetails unit test. */
class ReaderDetailsPhoneCollaborationTest {
    @Test
    void patchUpdatesIdentityBirthDatePhotoAndInterestsWithMatchingVersion() {
        // Arrange
        var sut=details(); var request=mock(UpdateReaderRequest.class);
        when(request.getUsername()).thenReturn("updated@example.org");
        when(request.getPassword()).thenReturn("NewPassw0rd!123");
        when(request.getFullName()).thenReturn("Updated Name");
        when(request.getBirthDate()).thenReturn("1990-02-03");
        var interests=java.util.List.of(mock(pt.psoft.g1.psoftg1.genremanagement.model.Genre.class));
        // Act
        sut.applyPatch(4L,request,"portrait.png",interests);
        // Assert
        verify(sut.getReader()).setUsername("updated@example.org");
        verify(sut.getReader()).setPassword("NewPassw0rd!123");
        verify(sut.getReader()).setName("Updated Name");
        assertEquals(java.time.LocalDate.of(1990,2,3),sut.getBirthDate().getBirthDate());
        assertEquals("portrait.png",sut.getPhoto().getPhotoFile());
        assertEquals(interests,sut.getInterestList());
    }

    @Test
    void photoRemovalRequiresCurrentVersion() {
        // Arrange
        var sut=details();
        sut.applyPatch(4L,mock(UpdateReaderRequest.class),"portrait.png",null);
        // Act / Assert
        assertThrows(ConflictException.class,()->sut.removePhoto(3L));
        assertEquals("portrait.png",sut.getPhoto().getPhotoFile());
        sut.removePhoto(4L);
        assertNull(sut.getPhoto());
    }
    private ReaderDetails details() {
        ReaderDetails details = new ReaderDetails(1, mock(Reader.class), "2000-01-01",
                "912345678", true, false, false, null, null);
        ReflectionTestUtils.setField(details, "version", 4L);
        return details;
    }

    @Test
    void validPatchReplacesPhoneAndUpdatesConsentFlags() {
        // Arrange
        ReaderDetails sut = details();
        UpdateReaderRequest request = mock(UpdateReaderRequest.class);
        when(request.getPhoneNumber()).thenReturn("212345678");
        when(request.getMarketing()).thenReturn(true);
        when(request.getThirdParty()).thenReturn(true);
        // Act
        sut.applyPatch(4L, request, null, null);
        // Assert
        assertEquals("212345678", sut.getPhoneNumber());
        assertTrue(sut.isMarketingConsent());
        assertTrue(sut.isThirdPartySharingConsent());
        verifyNoInteractions(sut.getReader());
    }

    @Test
    void invalidPhoneLeavesPreviousPhoneInPlace() {
        // Arrange
        ReaderDetails sut = details();
        UpdateReaderRequest request = mock(UpdateReaderRequest.class);
        when(request.getPhoneNumber()).thenReturn("91234567");
        // Act: capture the operation for the exception assertion.
        org.junit.jupiter.api.function.Executable action = () -> sut.applyPatch(4L, request, null, null);
        // Assert
        assertThrows(IllegalArgumentException.class, action);
        assertEquals("912345678", sut.getPhoneNumber());
    }

    @Test
    void staleVersionRejectsPatchBeforeReadingRequest() {
        // Arrange
        ReaderDetails sut = details();
        UpdateReaderRequest request = mock(UpdateReaderRequest.class);
        // Act: capture the operation for the exception assertion.
        org.junit.jupiter.api.function.Executable action = () -> sut.applyPatch(3L, request, null, null);
        // Assert
        assertThrows(ConflictException.class, action);
        assertEquals("912345678", sut.getPhoneNumber());
        verifyNoInteractions(request, sut.getReader());
    }

    @Test
    void omittedPhonePreservesExistingValue() {
        // Arrange
        ReaderDetails sut = details();
        UpdateReaderRequest request = mock(UpdateReaderRequest.class);
        // Act
        sut.applyPatch(4L, request, null, null);
        // Assert
        assertEquals("912345678", sut.getPhoneNumber());
    }
}
