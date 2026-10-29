package nl.ticketflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateTicketRequest(
        @NotBlank(message = "Titel is verplicht")
        @Size(max = 150, message = "Titel mag maximaal 150 tekens bevatten")
        String title,

        @NotBlank(message = "Omschrijving is verplicht")
        String description,

        UUID categoryId
) {
}
