package nl.ticketflow.service;

import nl.ticketflow.dto.request.CreateLabelRequest;
import nl.ticketflow.dto.response.LabelResponse;

import java.util.List;
import java.util.UUID;

public interface LabelService {

    List<LabelResponse> getAll();

    LabelResponse create(CreateLabelRequest request);

    void delete(UUID id);
}
