package pt.psoft.g1.psoftg1.readermanagement.services;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderView;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;

@Data
@Schema(description = "Reader with lending count")
public class ReaderAverageDto {
    @NotNull
    private ReaderDetails readerView;

    private Long lendingCount;
}
