package nl.ticketflow.service.impl;

import nl.ticketflow.dto.response.AttachmentDownload;
import nl.ticketflow.dto.response.AttachmentResponse;
import nl.ticketflow.entity.Attachment;
import nl.ticketflow.entity.Comment;
import nl.ticketflow.entity.Ticket;
import nl.ticketflow.entity.TicketStatus;
import nl.ticketflow.exception.BusinessRuleException;
import nl.ticketflow.exception.DuplicateResourceException;
import nl.ticketflow.exception.ForbiddenException;
import nl.ticketflow.exception.ResourceNotFoundException;
import nl.ticketflow.mapper.CommentMapper;
import nl.ticketflow.repository.AttachmentRepository;
import nl.ticketflow.repository.CommentRepository;
import nl.ticketflow.repository.TicketRepository;
import nl.ticketflow.security.CurrentUserService;
import nl.ticketflow.service.AttachmentService;
import nl.ticketflow.service.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.UUID;

@Service
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final CommentMapper commentMapper;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;

    public AttachmentServiceImpl(AttachmentRepository attachmentRepository, CommentRepository commentRepository,
                                 TicketRepository ticketRepository, CommentMapper commentMapper,
                                 CurrentUserService currentUserService, FileStorageService fileStorageService) {
        this.attachmentRepository = attachmentRepository;
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
        this.commentMapper = commentMapper;
        this.currentUserService = currentUserService;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional
    public AttachmentResponse upload(UUID ticketId, UUID commentId, MultipartFile file) {
        Ticket ticket = getAccessibleTicket(ticketId);
        Comment comment = getCommentOfTicket(ticket, commentId);
        checkNotClosed(ticket);
        if (comment.getAttachment() != null) {
            throw new DuplicateResourceException("Bij deze reactie is al een bijlage aanwezig");
        }

        // Validates type and size before anything is written to disk
        String storedName = fileStorageService.store(file);
        try {
            Attachment attachment = new Attachment();
            attachment.setFileName(StringUtils.getFilename(StringUtils.cleanPath(file.getOriginalFilename())));
            // store() accepted this type, so only the parameters and casing are normalized here
            attachment.setContentType(file.getContentType().split(";", 2)[0].trim().toLowerCase(Locale.ROOT));
            attachment.setSize(file.getSize());
            attachment.setStoragePath(storedName);
            // Keep both sides of the one-to-one in sync
            attachment.setComment(comment);
            comment.setAttachment(attachment);
            // Flush so database errors surface here and the stored file can still be cleaned up
            Attachment saved = attachmentRepository.saveAndFlush(attachment);
            return commentMapper.toAttachmentResponse(saved);
        } catch (RuntimeException e) {
            fileStorageService.delete(storedName);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentDownload download(UUID ticketId, UUID commentId) {
        Ticket ticket = getAccessibleTicket(ticketId);
        Attachment attachment = getAttachmentOrThrow(getCommentOfTicket(ticket, commentId));
        return new AttachmentDownload(fileStorageService.load(attachment.getStoragePath()),
                attachment.getFileName(), attachment.getContentType());
    }

    @Override
    @Transactional
    public void delete(UUID ticketId, UUID commentId) {
        Ticket ticket = getAccessibleTicket(ticketId);
        Comment comment = getCommentOfTicket(ticket, commentId);
        Attachment attachment = getAttachmentOrThrow(comment);
        checkNotClosed(ticket);

        String storagePath = attachment.getStoragePath();
        // orphanRemoval on Comment.attachment deletes the row
        comment.setAttachment(null);
        commentRepository.flush();
        fileStorageService.delete(storagePath);
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

    private Attachment getAttachmentOrThrow(Comment comment) {
        if (comment.getAttachment() == null) {
            throw new ResourceNotFoundException("Deze reactie heeft geen bijlage");
        }
        return comment.getAttachment();
    }

    private void checkNotClosed(Ticket ticket) {
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BusinessRuleException("Gesloten tickets accepteren geen bijlagen meer");
        }
    }
}
