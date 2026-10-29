package nl.ticketflow.service;

import nl.ticketflow.dto.request.CreateTicketRequest;
import nl.ticketflow.dto.request.UpdateTicketRequest;
import nl.ticketflow.dto.response.TicketResponse;
import nl.ticketflow.entity.TicketPriority;
import nl.ticketflow.entity.TicketStatus;

import java.util.List;
import java.util.UUID;

public interface TicketService {

    TicketResponse createTicket(CreateTicketRequest request);

    TicketResponse getTicket(UUID id);

    List<TicketResponse> listTickets(TicketStatus status, TicketPriority priority, UUID categoryId, UUID assigneeId);

    TicketResponse updateTicket(UUID id, UpdateTicketRequest request);
}
