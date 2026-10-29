package nl.ticketflow.service;

import nl.ticketflow.dto.response.AttachmentDownload;
import nl.ticketflow.dto.response.AttachmentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface AttachmentService {

    AttachmentResponse upload(UUID ticketId, UUID commentId, MultipartFile file);

    AttachmentDownload download(UUID ticketId, UUID commentId);

    void delete(UUID ticketId, UUID commentId);
}
