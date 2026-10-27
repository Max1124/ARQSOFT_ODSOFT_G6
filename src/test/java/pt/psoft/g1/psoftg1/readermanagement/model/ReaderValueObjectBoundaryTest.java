package pt.psoft.g1.psoftg1.readermanagement.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;

import java.time.DateTimeException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Domain class/unit tests: each test exercises one value-object class as SUT. */
class ReaderValueObjectBoundaryTest {
    @Test
    void leapDayIsPreserved() {
        // Arrange
        String date = "2000-02-29";
        // Act
        BirthDate sut = new BirthDate(date);
        // Assert
        assertEquals(LocalDate.of(2000, 2, 29), sut.getBirthDate());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2001-02-29", "2000-04-31", "2000-13-01", "2000-01-00"})
    void impossibleCalendarDatesAreRejected(String date) {
        // Arrange: date is supplied by the invalid calendar-date cases above.
        // Act: capture construction for the exception assertion.
        org.junit.jupiter.api.function.Executable action = () -> new BirthDate(date);
        // Assert
        assertThrows(DateTimeException.class, action);
    }

    @Test
    void directlyConstructedBirthDateAcceptsTodayButRejectsFutureDate() {
        // Arrange: current implementation has minimumAge=0 outside Spring injection.
        LocalDate today = LocalDate.now();
        // Act
        BirthDate sut = new BirthDate(today.toString());
        org.junit.jupiter.api.function.Executable futureDate = () -> new BirthDate(today.plusDays(2).toString());
        // Assert
        assertEquals(today, sut.getBirthDate());
        assertThrows(AccessDeniedException.class, futureDate);
    }

    @ParameterizedTest
    @ValueSource(strings = {"91234567", "9123456789", "21234567", "2123456789", "812345678", ""})
    void phoneLengthAndPrefixBoundariesAreRejected(String phone) {
        // Arrange: phone is supplied by the length/prefix boundary cases above.
        // Act: capture construction for the exception assertion.
        org.junit.jupiter.api.function.Executable action = () -> new PhoneNumber(phone);
        // Assert
        assertThrows(IllegalArgumentException.class, action);
    }

    @Test
    void explicitReaderNumberPreservesYearAndSequence() {
        // Arrange
        int year = 2024;
        int sequence = 123;
        // Act
        ReaderNumber sut = new ReaderNumber(year, sequence);
        // Assert
        assertEquals("2024/123", sut.toString());
    }
}
