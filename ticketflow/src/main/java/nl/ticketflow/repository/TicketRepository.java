package nl.ticketflow.repository;

import nl.ticketflow.entity.Ticket;
import nl.ticketflow.entity.TicketStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {

    Optional<Ticket> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByCategoryId(UUID categoryId);

    @Override
    @EntityGraph(attributePaths = {"reporter", "assignee", "category"})
    List<Ticket> findAll(Specification<Ticket> spec, Sort sort);

    // Most overdue first
    @EntityGraph(attributePaths = {"reporter", "assignee", "category"})
    @Query("select t from Ticket t where t.dueAt < :now and t.status not in :excludedStatuses "
            + "and t.deletedAt is null order by t.dueAt asc")
    List<Ticket> findOverdue(@Param("now") LocalDateTime now,
                             @Param("excludedStatuses") Collection<TicketStatus> excludedStatuses);

    @Query("select count(t) from Ticket t where t.dueAt < :now and t.status not in :excludedStatuses "
            + "and t.deletedAt is null")
    long countOverdue(@Param("now") LocalDateTime now,
                      @Param("excludedStatuses") Collection<TicketStatus> excludedStatuses);

    long countByDeletedAtIsNull();

    long countByAssigneeIsNullAndStatusNotInAndDeletedAtIsNull(Collection<TicketStatus> excludedStatuses);

    // Each row is [TicketStatus, Long]
    @Query("select t.status, count(t) from Ticket t where t.deletedAt is null group by t.status")
    List<Object[]> countPerStatus();

    // Each row is [TicketPriority, Long]
    @Query("select t.priority, count(t) from Ticket t where t.deletedAt is null group by t.priority")
    List<Object[]> countPerPriority();
}
