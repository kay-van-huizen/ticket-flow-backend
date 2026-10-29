package nl.ticketflow.repository;

import nl.ticketflow.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);

    List<Comment> findByTicketIdAndInternalFalseOrderByCreatedAtAsc(UUID ticketId);
}
