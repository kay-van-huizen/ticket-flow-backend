package nl.ticketflow.service.impl;

import nl.ticketflow.entity.UserAccount;
import nl.ticketflow.entity.UserRole;
import nl.ticketflow.exception.BusinessRuleException;
import nl.ticketflow.repository.UserAccountRepository;
import nl.ticketflow.security.CurrentUserService;
import nl.ticketflow.service.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private static final String USERNAME_CLAIM = "preferred_username";
    private static final String EMAIL_CLAIM = "email";
    private static final String NAME_CLAIM = "name";

    private final UserAccountRepository userAccountRepository;
    private final CurrentUserService currentUserService;
    private final TransactionTemplate newTransaction;

    public UserServiceImpl(UserAccountRepository userAccountRepository, CurrentUserService currentUserService,
                           PlatformTransactionManager transactionManager) {
        this.userAccountRepository = userAccountRepository;
        this.currentUserService = currentUserService;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
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
        if (username == null || username.isBlank()) {
            throw new BusinessRuleException("Je account heeft geen gebruikersnaam. Vraag een beheerder om dit in te stellen.");
        }

        String fullName = jwt.getClaimAsString(NAME_CLAIM);

        UserAccount user = new UserAccount();
        user.setId(userId);
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName == null || fullName.isBlank() ? username : fullName);
        user.setRole(role);

        // Own transaction, so a failed insert does not mark the caller's transaction rollback-only
        try {
            newTransaction.executeWithoutResult(status -> userAccountRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException ex) {
            // A concurrent first request just created this user; read that one instead
            return userAccountRepository.findById(userId).orElseThrow(() -> ex);
        }
        return userAccountRepository.findById(userId).orElseThrow();
    }
}
