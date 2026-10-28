package nl.ticketflow.mapper;

import nl.ticketflow.dto.response.UserResponse;
import nl.ticketflow.entity.UserAccount;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(UserAccount user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole()
        );
    }
}
