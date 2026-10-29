package nl.ticketflow.service;

import nl.ticketflow.dto.request.CreateTicketRequest;
import nl.ticketflow.dto.response.TicketResponse;

import java.util.UUID;

public interface TicketService {

    TicketResponse createTicket(CreateTicketRequest request);

    TicketResponse getTicket(UUID id);
}
