package pt.psoft.g1.psoftg1.genremanagement.api;

import java.util.List;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.bookmanagement.services.GenreBookCountDTO;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.genremanagement.services.*;
import pt.psoft.g1.psoftg1.shared.services.Page;
import pt.psoft.g1.psoftg1.shared.services.SearchRequest;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** SUT: GenreController alone; service and mapper are mocks. HTTP/security are tested in the component suite. */
class GenreControllerTest {
    private final GenreService service = mock(GenreService.class);
    private final GenreViewMapper mapper = mock(GenreViewMapper.class);
    private final GenreController sut = new GenreController(service, mapper);

    @Test
    void wrapsMappedTopGenres() {
        // Arrange
        var rows = List.of(new GenreBookCountDTO("Fiction", 8));
        var views = List.of(new GenreBookCountView());
        when(service.findTopGenreByBooks()).thenReturn(rows);
        when(mapper.toGenreBookCountView(rows)).thenReturn(views);
        // Act
        var result = sut.getTop();
        // Assert
        assertThat(result.getItems()).isSameAs(views);
        verify(mapper).toGenreBookCountView(rows);
    }

    @Test
    void emptyTopGenresAreNotMapped() {
        // Arrange
        when(service.findTopGenreByBooks()).thenReturn(List.of());
        // Act / Assert
        assertThatThrownBy(sut::getTop).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(mapper);
    }

    @Test
    void emptyMonthlyCountsAreNotMapped() {
        // Arrange
        when(service.getLendingsPerMonthLastYearByGenre()).thenReturn(List.of());
        // Act / Assert
        assertThatThrownBy(sut::getLendingsPerMonthLastYearByGenre).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(mapper);
    }

    @Test
    void emptyDurationsAreNotMapped() {
        // Arrange
        when(service.getLendingsAverageDurationPerMonth("2024-01-01", "2024-02-01")).thenReturn(List.of());
        // Act / Assert
        assertThatThrownBy(() -> sut.getLendingsAverageDurationPerMonth("2024-01-01", "2024-02-01"))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(mapper);
    }

    @Test
    void averageSearchForwardsQueryAndPageAndAllowsEmptyResults() {
        // Arrange
        var query = new GetAverageLendingsQuery(2024, 2);
        var page = new Page(2, 5);
        List<GenreLendingsDTO> rows = List.of();
        when(service.getAverageLendings(query, page)).thenReturn(rows);
        when(mapper.toGenreAvgLendingsView(rows)).thenReturn(List.of());
        // Act
        var result = sut.getAverageLendings(new SearchRequest<>(page, query));
        // Assert
        assertThat(result.getItems()).isEmpty();
        verify(service).getAverageLendings(same(query), same(page));
        verify(mapper).toGenreAvgLendingsView(rows);
    }
}
