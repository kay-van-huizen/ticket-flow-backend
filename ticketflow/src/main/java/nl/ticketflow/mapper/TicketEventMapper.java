package nl.ticketflow.mapper;

import nl.ticketflow.dto.response.TicketEventResponse;
import nl.ticketflow.entity.TicketEvent;
import org.springframework.stereotype.Component;

@Component
public class TicketEventMapper {

    public TicketEventResponse toResponse(TicketEvent event) {
        return new TicketEventResponse(
                event.getId(),
                event.getType(),
                event.getOldValue(),
                event.getNewValue(),
                event.getChangedBy().getUsername(),
                event.getChangedAt()
        );
    }
}
