package nl.ticketflow.mapper;

import nl.ticketflow.dto.request.CreateTicketRequest;
import nl.ticketflow.dto.request.UpdateTicketRequest;
import nl.ticketflow.dto.response.TicketResponse;
import nl.ticketflow.entity.Category;
import nl.ticketflow.entity.Ticket;
import nl.ticketflow.entity.UserAccount;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {

    public Ticket toEntity(CreateTicketRequest request, UserAccount reporter, Category category) {
        Ticket ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setReporter(reporter);
        ticket.setCategory(category);
        return ticket;
    }

    // PUT replaces the whole resource, so a null category clears it
    public void updateEntity(Ticket ticket, UpdateTicketRequest request, Category category) {
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setCategory(category);
    }

    public TicketResponse toResponse(Ticket ticket) {
        UserAccount reporter = ticket.getReporter();
        UserAccount assignee = ticket.getAssignee();
        Category category = ticket.getCategory();
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getStatus(),
                reporter.getId(),
                reporter.getFullName(),
                assignee == null ? null : assignee.getId(),
                assignee == null ? null : assignee.getFullName(),
                category == null ? null : category.getId(),
                category == null ? null : category.getTitle(),
                ticket.getDueAt(),
                ticket.getResolvedAt(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
