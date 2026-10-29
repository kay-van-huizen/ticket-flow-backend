package nl.ticketflow.dto.response;

import nl.ticketflow.entity.TicketPriority;
import nl.ticketflow.entity.TicketStatus;

import java.util.Map;

public record TicketStatsResponse(
        long total,
        Map<TicketStatus, Long> byStatus,
        Map<TicketPriority, Long> byPriority,
        long overdue,
        long unassigned
) {
}
