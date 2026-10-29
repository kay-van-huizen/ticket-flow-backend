package nl.ticketflow.service.impl;

import nl.ticketflow.entity.Ticket;
import nl.ticketflow.entity.TicketEvent;
import nl.ticketflow.entity.TicketEventType;
import nl.ticketflow.entity.UserAccount;
import nl.ticketflow.repository.TicketEventRepository;
import org.springframework.stereotype.Component;

@Component
public class TicketEventRecorder {

    private final TicketEventRepository ticketEventRepository;

    public TicketEventRecorder(TicketEventRepository ticketEventRepository) {
        this.ticketEventRepository = ticketEventRepository;
    }

    public void record(Ticket ticket, TicketEventType type, String oldValue, String newValue, UserAccount changedBy) {
        TicketEvent event = new TicketEvent();
        event.setTicket(ticket);
        event.setType(type);
        event.setOldValue(oldValue);
        event.setNewValue(newValue);
        event.setChangedBy(changedBy);
        ticketEventRepository.save(event);
    }
}
