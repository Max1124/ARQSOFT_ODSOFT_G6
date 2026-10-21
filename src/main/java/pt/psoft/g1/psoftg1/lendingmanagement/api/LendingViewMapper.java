package pt.psoft.g1.psoftg1.lendingmanagement.api;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.shared.api.MapperInterface;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;

/**
 * Brief guides:
 * <a href="https://www.baeldung.com/mapstruct">https://www.baeldung.com/mapstruct</a>
 * <p>
 * <a href="https://medium.com/@susantamon/mapstruct-a-comprehensive-guide-in-spring-boot-context-1e7202da033e">https://medium.com/@susantamon/mapstruct-a-comprehensive-guide-in-spring-boot-context-1e7202da033e</a>
 * */
@Mapper(componentModel = "spring")
public abstract class LendingViewMapper extends MapperInterface {

    @Autowired
    private Clock clock; // the entity no longer reads the system clock, so the view supplies the date

    protected LocalDate today() {
        return LocalDate.now(clock);
    }

    @Mapping(target = "lendingNumber", source = "lendingNumber")
    @Mapping(target = "bookTitle", source = "book.title")
    @Mapping(target = "daysUntilReturn", expression = "java(lending.getDaysUntilReturn(today()).orElse(null))")
    @Mapping(target = "daysOverdue", expression = "java(lending.getDaysOverdue(today()).orElse(null))")
    @Mapping(target = "fineValueInCents", expression = "java(lending.getFineValueInCents(today()).orElse(null))")
    @Mapping(target = "_links.self", source = ".", qualifiedByName = "lendingLink")
    @Mapping(target = "_links.book", source = "book", qualifiedByName = "bookLink")
    @Mapping(target = "returnedDate", source = "returnedDate")
    @Mapping(target = "_links.reader", source = "readerDetails", qualifiedByName = "readerLink")
    public abstract LendingView toLendingView(Lending lending);

    public abstract List<LendingView> toLendingView(List<Lending> lendings);

    public abstract LendingsAverageDurationView toLendingsAverageDurationView(Double lendingsAverageDuration);
}
