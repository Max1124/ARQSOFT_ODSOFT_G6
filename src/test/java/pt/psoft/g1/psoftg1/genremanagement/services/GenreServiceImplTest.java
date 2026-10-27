package pt.psoft.g1.psoftg1.genremanagement.services;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import pt.psoft.g1.psoftg1.bookmanagement.services.GenreBookCountDTO;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.shared.services.Page;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** SUT: GenreServiceImpl; isolated class tests with its repository replaced by a mock. */
class GenreServiceImplTest {
    private final GenreRepository repository = mock(GenreRepository.class);
    private final GenreServiceImpl sut = new GenreServiceImpl(repository);

    @Test
    void topGenresRequestsOnlyTheFirstFiveResults() {
        // Arrange
        var rows = List.of(new GenreBookCountDTO("Fiction", 7));
        when(repository.findTop5GenreByBookCount(any())).thenReturn(new PageImpl<>(rows));
        // Act
        var result = sut.findTopGenreByBooks();
        // Assert
        assertThat(result).containsExactlyElementsOf(rows);
        var page = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findTop5GenreByBookCount(page.capture());
        assertThat(page.getValue().getPageNumber()).isZero();
        assertThat(page.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    void missingPageDefaultsToFirstTenAndMonthStartsOnDayOne() {
        // Arrange
        var rows = List.of(new GenreLendingsDTO("Fiction", 0.5));
        when(repository.getAverageLendingsInMonth(any(), any())).thenReturn(rows);
        // Act
        var result = sut.getAverageLendings(new GetAverageLendingsQuery(2024, 2), null);
        // Assert
        assertThat(result).isSameAs(rows);
        verify(repository).getAverageLendingsInMonth(LocalDate.of(2024, 2, 1), new Page(1, 10));
    }

    @Test
    void explicitPageIsPreserved() {
        // Arrange
        Page page = new Page(3, 2);
        when(repository.getAverageLendingsInMonth(any(), any())).thenReturn(List.of());
        // Act
        var result = sut.getAverageLendings(new GetAverageLendingsQuery(2025, 12), page);
        // Assert
        assertThat(result).isEmpty();
        verify(repository).getAverageLendingsInMonth(eq(LocalDate.of(2025, 12, 1)), same(page));
    }

    @ParameterizedTest
    @CsvSource({"2024-02-30,2024-03-01", "2024-01-01,invalid", "01-01-2024,2024-03-01", "2024-03-02,2024-03-01"})
    void rejectsInvalidOrReversedDatesBeforeConsultingRepository(String start, String end) {
        // Arrange: invalid interval supplied by the parameter source.
        // Act / Assert
        assertThatIllegalArgumentException().isThrownBy(() -> sut.getLendingsAverageDurationPerMonth(start, end));
        verifyNoInteractions(repository);
    }

    @Test
    void acceptsEqualStartAndEndDates() {
        // Arrange
        LocalDate date = LocalDate.of(2024, 2, 29);
        var rows = List.of(new GenreLendingsPerMonthDTO(2024, 2, List.of(new GenreLendingsDTO("Fiction", 2.0))));
        when(repository.getLendingsAverageDurationPerMonth(date, date)).thenReturn(rows);
        // Act
        var result = sut.getLendingsAverageDurationPerMonth("2024-02-29", "2024-02-29");
        // Assert
        assertThat(result).isSameAs(rows);
        verify(repository).getLendingsAverageDurationPerMonth(date, date);
    }

    @Test
    void reportsNoDurationDataAsNotFound() {
        // Arrange
        when(repository.getLendingsAverageDurationPerMonth(any(), any())).thenReturn(List.of());
        // Act / Assert
        assertThatThrownBy(() -> sut.getLendingsAverageDurationPerMonth("2024-01-01", "2024-02-01"))
                .isInstanceOf(NotFoundException.class).hasMessage("No objects match the provided criteria");
    }
}
