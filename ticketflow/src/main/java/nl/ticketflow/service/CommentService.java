package nl.ticketflow.service;

import nl.ticketflow.dto.request.CreateCommentRequest;
import nl.ticketflow.dto.request.UpdateCommentRequest;
import nl.ticketflow.dto.response.CommentResponse;

import java.util.List;
import java.util.UUID;

public interface CommentService {

    List<CommentResponse> list(UUID ticketId);

    CommentResponse create(UUID ticketId, CreateCommentRequest request);

    CommentResponse update(UUID ticketId, UUID commentId, UpdateCommentRequest request);

    void delete(UUID ticketId, UUID commentId);
}
