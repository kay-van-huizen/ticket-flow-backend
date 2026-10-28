package nl.ticketflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import nl.ticketflow.entity.Color;

import java.util.UUID;

public record CreateCategoryRequest(
        @NotBlank(message = "Titel is verplicht")
        @Size(max = 100, message = "Titel mag maximaal 100 tekens bevatten")
        String title,

        // Column is TEXT, so this limit only applies at the API level
        @Size(max = 1000, message = "Beschrijving mag maximaal 1000 tekens bevatten")
        String description,

        Color color,

        UUID parentId
) {
}
