package nl.ticketflow.repository;

import nl.ticketflow.entity.Ticket;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {

    Optional<Ticket> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByCategoryId(UUID categoryId);

    @Override
    @EntityGraph(attributePaths = {"reporter", "assignee", "category"})
    List<Ticket> findAll(Specification<Ticket> spec, Sort sort);
}
