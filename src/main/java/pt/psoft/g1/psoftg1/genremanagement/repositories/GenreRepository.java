package pt.psoft.g1.psoftg1.genremanagement.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pt.psoft.g1.psoftg1.bookmanagement.services.GenreBookCountDTO;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreLendingsDTO;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreLendingsPerMonthDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GenreRepository {

    Iterable<Genre> findAll();
    Optional<Genre> findByString(String genreName);
    Genre save(Genre genre);
    Page<GenreBookCountDTO> findTop5GenreByBookCount(Pageable pageable);
    List<GenreLendingsDTO> getAverageLendingsInMonth(LocalDate month, pt.psoft.g1.psoftg1.shared.services.Page page);
    List<GenreLendingsPerMonthDTO> getLendingsPerMonthLastYearByGenre();
    List<GenreLendingsPerMonthDTO> getLendingsAverageDurationPerMonth(LocalDate startDate, LocalDate endDate);
    void delete(Genre genre);
    boolean isUsedByBooks(Genre genre);
    boolean isUsedByReaders(Genre genre);
}
