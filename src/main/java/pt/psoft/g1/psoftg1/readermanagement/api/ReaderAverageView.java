package pt.psoft.g1.psoftg1.readermanagement.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Reader with lending count")
public class ReaderAverageView {
    @NotNull
    private ReaderView readerView;

    private Long lendingCount;
}
