package nl.ticketflow.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import nl.ticketflow.entity.TicketStatus;

public record ChangeStatusRequest(
        @NotNull(message = "Status is verplicht")
        TicketStatus status,

        @Size(max = 500, message = "Reden mag maximaal 500 tekens bevatten")
        String reason
) {
}
