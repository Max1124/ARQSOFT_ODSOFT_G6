package pt.psoft.g1.psoftg1.genremanagement.services;

import pt.psoft.g1.psoftg1.bookmanagement.services.GenreBookCountDTO;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.shared.services.Page;

import java.util.List;
import java.util.Optional;

public interface GenreService {
    Iterable<Genre> findAll();
    Genre create(String name);
    Genre update(String currentName, String newName);
    void delete(String name);
    Genre save(Genre genre);
    Optional<Genre> findByString(String name);
    List<GenreBookCountDTO> findTopGenreByBooks();
    List<GenreLendingsDTO> getAverageLendings(GetAverageLendingsQuery query, Page page);
    List<GenreLendingsPerMonthDTO> getLendingsPerMonthLastYearByGenre();
    List<GenreLendingsPerMonthDTO> getLendingsAverageDurationPerMonth(String startDate, String endDate);
}
