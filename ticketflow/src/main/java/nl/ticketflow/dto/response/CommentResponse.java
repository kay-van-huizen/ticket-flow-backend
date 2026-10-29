package nl.ticketflow.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID ticketId,
        UUID authorId,
        String authorName,
        String body,
        boolean internal,
        LocalDateTime createdAt,
        AttachmentResponse attachment
) {
}
