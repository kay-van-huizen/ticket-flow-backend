package nl.ticketflow.dto.response;

import nl.ticketflow.entity.TicketEventType;

import java.time.LocalDateTime;
import java.util.UUID;

public record TicketEventResponse(
        UUID id,
        TicketEventType type,
        String oldValue,
        String newValue,
        String changedBy,
        LocalDateTime changedAt
) {
}
