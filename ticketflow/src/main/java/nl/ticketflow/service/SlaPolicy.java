package nl.ticketflow.service;

import nl.ticketflow.entity.TicketPriority;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class SlaPolicy {

    /**
     * Returns the SLA duration for the given priority.
     */
    public Duration getDuration(TicketPriority priority) {
        // No default branch: adding a new priority must fail compilation until it is mapped here
        return switch (priority) {
            case LOW -> Duration.ofDays(7);
            case NORMAL -> Duration.ofDays(3);
            case HIGH -> Duration.ofDays(1);
            case CRITICAL -> Duration.ofHours(4);
        };
    }

    /**
     * Calculates the due date by adding the SLA duration of the priority to the creation time.
     */
    public LocalDateTime calculateDueAt(LocalDateTime createdAt, TicketPriority priority) {
        return createdAt.plus(getDuration(priority));
    }
}
