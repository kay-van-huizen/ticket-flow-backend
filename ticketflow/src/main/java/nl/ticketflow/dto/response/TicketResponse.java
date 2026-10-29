package nl.ticketflow.dto.response;

import nl.ticketflow.entity.TicketPriority;
import nl.ticketflow.entity.TicketStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        String title,
        String description,
        TicketPriority priority,
        TicketStatus status,
        UUID reporterId,
        String reporterName,
        UUID assigneeId,
        String assigneeName,
        UUID categoryId,
        String categoryTitle,
        LocalDateTime dueAt,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
