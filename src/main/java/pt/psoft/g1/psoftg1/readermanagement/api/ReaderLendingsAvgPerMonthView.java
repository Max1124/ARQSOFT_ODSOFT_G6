package pt.psoft.g1.psoftg1.readermanagement.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import pt.psoft.g1.psoftg1.genremanagement.api.GenreLendingsView;

import java.util.List;

@Data
@Schema(description = "A Genre and its lending duration averages per month.")
@AllArgsConstructor
public class ReaderLendingsAvgPerMonthView {
    private Integer year;
    private Integer month;
    private List<ReaderAverageView> durationAverages;

}
