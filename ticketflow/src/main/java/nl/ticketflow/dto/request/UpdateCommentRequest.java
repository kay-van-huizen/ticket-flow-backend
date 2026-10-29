package nl.ticketflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCommentRequest(
        @NotBlank(message = "Reactie mag niet leeg zijn")
        @Size(max = 2000, message = "Reactie mag maximaal 2000 tekens bevatten")
        String body
) {
}
