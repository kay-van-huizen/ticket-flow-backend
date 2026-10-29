package nl.ticketflow.dto.request;

import jakarta.validation.constraints.NotNull;
import nl.ticketflow.entity.TicketPriority;

public record ChangePriorityRequest(
        @NotNull(message = "Prioriteit is verplicht")
        TicketPriority priority
) {
}
