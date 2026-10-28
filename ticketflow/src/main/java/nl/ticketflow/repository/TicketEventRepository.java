package nl.ticketflow.repository;

import nl.ticketflow.entity.TicketEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TicketEventRepository extends JpaRepository<TicketEvent, UUID> {
}
