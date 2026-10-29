package nl.ticketflow.dto.response;

import nl.ticketflow.entity.Color;

import java.util.UUID;

public record LabelResponse(
        UUID id,
        String name,
        Color color
) {
}
