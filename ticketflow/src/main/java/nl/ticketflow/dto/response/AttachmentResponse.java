package nl.ticketflow.dto.response;

import java.util.UUID;

public record AttachmentResponse(
        UUID id,
        String fileName,
        String contentType,
        Long size
) {
}
