package nl.ticketflow.dto.response;

import nl.ticketflow.entity.UserRole;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        String fullName,
        UserRole role
) {
}
