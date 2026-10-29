package nl.ticketflow.repository;

import nl.ticketflow.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface LabelRepository extends JpaRepository<Label, UUID> {

    boolean existsByNameIgnoreCase(String name);

    // Also counts soft-deleted tickets, their ticket_labels rows still reference the label
    @Query("select count(t) > 0 from Ticket t join t.labels l where l.id = :labelId")
    boolean isInUse(@Param("labelId") UUID labelId);
}
