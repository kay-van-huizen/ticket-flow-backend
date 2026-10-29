package nl.ticketflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import nl.ticketflow.entity.Color;

public record CreateLabelRequest(
        @NotBlank(message = "Naam is verplicht")
        @Size(max = 40, message = "Naam mag maximaal 40 tekens bevatten")
        String name,

        Color color
) {
}
