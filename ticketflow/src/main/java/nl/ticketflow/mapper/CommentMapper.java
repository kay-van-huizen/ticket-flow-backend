package nl.ticketflow.mapper;

import nl.ticketflow.dto.request.CreateCommentRequest;
import nl.ticketflow.dto.response.AttachmentResponse;
import nl.ticketflow.dto.response.CommentResponse;
import nl.ticketflow.entity.Attachment;
import nl.ticketflow.entity.Comment;
import nl.ticketflow.entity.Ticket;
import nl.ticketflow.entity.UserAccount;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CommentMapper {

    public Comment toEntity(CreateCommentRequest request, Ticket ticket, UserAccount author) {
        Comment comment = new Comment();
        comment.setBody(request.body());
        comment.setInternal(request.internal());
        comment.setTicket(ticket);
        comment.setAuthor(author);
        return comment;
    }

    public CommentResponse toResponse(Comment comment) {
        UserAccount author = comment.getAuthor();
        return new CommentResponse(
                comment.getId(),
                comment.getTicket().getId(),
                author.getId(),
                author.getFullName(),
                comment.getBody(),
                comment.isInternal(),
                comment.getCreatedAt(),
                toAttachmentResponse(comment.getAttachment())
        );
    }

    public List<CommentResponse> toResponses(List<Comment> comments) {
        return comments.stream()
                .map(this::toResponse)
                .toList();
    }

    public AttachmentResponse toAttachmentResponse(Attachment attachment) {
        if (attachment == null) {
            return null;
        }
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getSize()
        );
    }
}
