package nl.ticketflow.service.impl;

import nl.ticketflow.dto.request.CreateCommentRequest;
import nl.ticketflow.dto.request.UpdateCommentRequest;
import nl.ticketflow.dto.response.CommentResponse;
import nl.ticketflow.entity.Comment;
import nl.ticketflow.entity.Ticket;
import nl.ticketflow.entity.TicketStatus;
import nl.ticketflow.entity.UserAccount;
import nl.ticketflow.entity.UserRole;
import nl.ticketflow.exception.BusinessRuleException;
import nl.ticketflow.exception.ForbiddenException;
import nl.ticketflow.exception.ResourceNotFoundException;
import nl.ticketflow.mapper.CommentMapper;
import nl.ticketflow.repository.CommentRepository;
import nl.ticketflow.repository.TicketRepository;
import nl.ticketflow.security.CurrentUserService;
import nl.ticketflow.service.CommentService;
import nl.ticketflow.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final CommentMapper commentMapper;
    private final CurrentUserService currentUserService;
    private final UserService userService;

    public CommentServiceImpl(CommentRepository commentRepository, TicketRepository ticketRepository,
                              CommentMapper commentMapper, CurrentUserService currentUserService,
                              UserService userService) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
        this.commentMapper = commentMapper;
        this.currentUserService = currentUserService;
        this.userService = userService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> list(UUID ticketId) {
        Ticket ticket = getAccessibleTicket(ticketId);
        // Internal comments must never reach a REPORTER, so they are filtered here and not in the controller
        List<Comment> comments = currentUserService.isAgentOrAdmin()
                ? commentRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId())
                : commentRepository.findByTicketIdAndInternalFalseOrderByCreatedAtAsc(ticket.getId());
        return commentMapper.toResponses(comments);
    }

    @Override
    @Transactional
    public CommentResponse create(UUID ticketId, CreateCommentRequest request) {
        Ticket ticket = getAccessibleTicket(ticketId);
        if (!currentUserService.isAgentOrAdmin() && request.internal()) {
            throw new ForbiddenException("Melders mogen geen interne reacties plaatsen");
        }
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BusinessRuleException("Gesloten tickets accepteren geen reacties meer");
        }

        // Creates the UserAccount on first login, so the author always exists in the database
        UserAccount author = userService.getOrCreateCurrentUser();
        Comment comment = commentMapper.toEntity(request, ticket, author);
        return commentMapper.toResponse(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public CommentResponse update(UUID ticketId, UUID commentId, UpdateCommentRequest request) {
        Ticket ticket = getAccessibleTicket(ticketId);
        Comment comment = getCommentOfTicket(ticket, commentId);
        if (!isAuthorOrAdmin(comment)) {
            throw new ForbiddenException("Je mag alleen je eigen reacties wijzigen");
        }

        // Only the body can change; internal stays as it was created
        comment.setBody(request.body());
        return commentMapper.toResponse(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public void delete(UUID ticketId, UUID commentId) {
        Ticket ticket = getAccessibleTicket(ticketId);
        Comment comment = getCommentOfTicket(ticket, commentId);
        if (!isAuthorOrAdmin(comment)) {
            throw new ForbiddenException("Je mag alleen je eigen reacties verwijderen");
        }

        // CascadeType.ALL on Comment.attachment removes the attachment row as well
        commentRepository.delete(comment);
    }

    // Soft-deleted tickets give 404; a REPORTER gets 403 on someone else's ticket
    private Ticket getAccessibleTicket(UUID ticketId) {
        Ticket ticket = ticketRepository.findByIdAndDeletedAtIsNull(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket niet gevonden"));
        if (!currentUserService.isAgentOrAdmin()
                && !ticket.getReporter().getId().equals(currentUserService.getCurrentUserId())) {
            throw new ForbiddenException("Je hebt geen toegang tot dit ticket");
        }
        return ticket;
    }

    // A comment of another ticket, or an internal comment for a REPORTER, gives 404 so its existence is not revealed
    private Comment getCommentOfTicket(Ticket ticket, UUID commentId) {
        return commentRepository.findById(commentId)
                .filter(comment -> comment.getTicket().getId().equals(ticket.getId()))
                .filter(comment -> currentUserService.isAgentOrAdmin() || !comment.isInternal())
                .orElseThrow(() -> new ResourceNotFoundException("Reactie met id " + commentId + " is niet gevonden"));
    }

    private boolean isAuthorOrAdmin(Comment comment) {
        return comment.getAuthor().getId().equals(currentUserService.getCurrentUserId())
                || currentUserService.getCurrentRole() == UserRole.ADMIN;
    }
}
