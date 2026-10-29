package nl.ticketflow.dto.response;

import org.springframework.core.io.Resource;

public record AttachmentDownload(
        Resource resource,
        String fileName,
        String contentType
) {
}
