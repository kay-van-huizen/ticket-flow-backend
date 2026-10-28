package nl.ticketflow.dto.request;

import jakarta.validation.constraints.NotNull;
import nl.ticketflow.entity.CategoryStatus;

public record ChangeCategoryStatusRequest(
        @NotNull(message = "Status is verplicht")
        CategoryStatus status
) {
}
