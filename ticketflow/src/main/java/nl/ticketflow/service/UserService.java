package nl.ticketflow.service;

import nl.ticketflow.entity.UserAccount;
import nl.ticketflow.entity.UserRole;

import java.util.List;

public interface UserService {

    UserAccount getOrCreateCurrentUser();

    List<UserAccount> findByRole(UserRole role);
}
