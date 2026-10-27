package pt.psoft.g1.psoftg1.genremanagement.api;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import pt.psoft.g1.psoftg1.bookmanagement.services.GenreBookCountDTO;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.services.*;
import static org.assertj.core.api.Assertions.*;

/** SUT: generated GenreViewMapper with real domain/DTO/view collaborators; in-process collaboration, not an isolated unit. */
class GenreViewMapperCollaborationTest {
    private final GenreViewMapper sut = Mappers.getMapper(GenreViewMapper.class);

    @Test
    void mapsDomainNameAndNestedBookCountsInOrder() {
        // Arrange
        var genre = new Genre("Fiction");
        var rows = List.of(new GenreBookCountDTO("Fiction", 8), new GenreBookCountDTO("History", 3));
        // Act
        var view = sut.toGenreView(genre);
        var counts = sut.toGenreBookCountView(rows);
        // Assert
        assertThat(view.getGenre()).isEqualTo("Fiction");
        assertThat(counts).extracting(v -> v.getGenreView().getGenre()).containsExactly("Fiction", "History");
        assertThat(counts).extracting(GenreBookCountView::getBookCount).containsExactly(8L, 3L);
    }

    @Test
    void mapsMonthlyCountsAndDurationsToTheirDistinctFields() {
        // Arrange
        var counts = new GenreLendingsPerMonthDTO(2024, 2, List.of(new GenreLendingsDTO("Fiction", 4L)));
        var durations = new GenreLendingsPerMonthDTO(2025, 12, List.of(new GenreLendingsDTO("History", 2.26)));
        // Act
        var countView = sut.toGenreLendingsCountPerMonthView(List.of(counts)).get(0);
        var durationView = sut.toGenreLendingsAveragePerMonthView(List.of(durations)).get(0);
        // Assert
        assertThat(countView.getYear()).isEqualTo(2024);
        assertThat(countView.getMonth()).isEqualTo(2);
        assertThat(countView.getLendingsCount()).singleElement().satisfies(v -> {
            assertThat(v.getGenre()).isEqualTo("Fiction");
            assertThat(v.getValue()).isEqualTo(4L);
        });
        assertThat(durationView.getYear()).isEqualTo(2025);
        assertThat(durationView.getMonth()).isEqualTo(12);
        assertThat(durationView.getDurationAverages()).singleElement().satisfies(v -> {
            assertThat(v.getGenre()).isEqualTo("History");
            assertThat(v.getValue()).isEqualTo(2.3);
        });
    }
}
