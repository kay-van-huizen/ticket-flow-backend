package nl.ticketflow.dto.response;

import nl.ticketflow.entity.CategoryStatus;
import nl.ticketflow.entity.Color;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String title,
        String description,
        Color color,
        CategoryStatus status,
        UUID parentId
) {
}
