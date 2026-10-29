package nl.ticketflow.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignTicketRequest(
        @NotNull(message = "Assignee is verplicht")
        UUID assigneeId
) {
}
