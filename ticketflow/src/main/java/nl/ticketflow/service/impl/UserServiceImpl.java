package nl.ticketflow.service.impl;

import nl.ticketflow.entity.UserAccount;
import nl.ticketflow.entity.UserRole;
import nl.ticketflow.exception.BusinessRuleException;
import nl.ticketflow.repository.UserAccountRepository;
import nl.ticketflow.security.CurrentUserService;
import nl.ticketflow.service.UserService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private static final String USERNAME_CLAIM = "preferred_username";
    private static final String EMAIL_CLAIM = "email";
    private static final String NAME_CLAIM = "name";

    private final UserAccountRepository userAccountRepository;
    private final CurrentUserService currentUserService;

    public UserServiceImpl(UserAccountRepository userAccountRepository, CurrentUserService currentUserService) {
        this.userAccountRepository = userAccountRepository;
        this.currentUserService = currentUserService;
    }

    @Override
    @Transactional
    public UserAccount getOrCreateCurrentUser() {
        UUID userId = currentUserService.getCurrentUserId();
        UserRole tokenRole = currentUserService.getCurrentRole();

        return userAccountRepository.findById(userId)
                .map(user -> {
                    if (user.getRole() != tokenRole) {
                        user.setRole(tokenRole);
                    }
                    return user;
                })
                .orElseGet(() -> createUser(userId, tokenRole));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAccount> findByRole(UserRole role) {
        return userAccountRepository.findByRole(role);
    }

    private UserAccount createUser(UUID userId, UserRole role) {
        Jwt jwt = currentUserService.getJwt();

        String email = jwt.getClaimAsString(EMAIL_CLAIM);
        if (email == null || email.isBlank()) {
            throw new BusinessRuleException("Je account heeft geen e-mailadres. Vraag een beheerder om dit in te stellen.");
        }

        String username = jwt.getClaimAsString(USERNAME_CLAIM);
        String fullName = jwt.getClaimAsString(NAME_CLAIM);

        UserAccount user = new UserAccount();
        user.setId(userId);
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName == null || fullName.isBlank() ? username : fullName);
        user.setRole(role);
        return userAccountRepository.save(user);
    }
}
