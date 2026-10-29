package nl.ticketflow.repository;

import nl.ticketflow.entity.Ticket;
import nl.ticketflow.entity.TicketPriority;
import nl.ticketflow.entity.TicketStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

// Optional filters as separate predicates, fails on PostgreSQL for null enums and UUIDs
public final class TicketSpecifications {

    private TicketSpecifications() {
    }

    public static Specification<Ticket> hasStatus(TicketStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Ticket> hasPriority(TicketPriority priority) {
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    public static Specification<Ticket> hasCategory(UUID categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Ticket> hasAssignee(UUID assigneeId) {
        return (root, query, cb) -> cb.equal(root.get("assignee").get("id"), assigneeId);
    }

    // Joins on one label id, so each ticket appears at most once
    public static Specification<Ticket> hasLabel(UUID labelId) {
        return (root, query, cb) -> cb.equal(root.join("labels").get("id"), labelId);
    }

    public static Specification<Ticket> hasReporter(UUID reporterId) {
        return (root, query, cb) -> cb.equal(root.get("reporter").get("id"), reporterId);
    }

    public static Specification<Ticket> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }
}
