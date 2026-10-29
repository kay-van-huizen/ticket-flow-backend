package nl.ticketflow.security;

import nl.ticketflow.entity.UserRole;
import nl.ticketflow.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CurrentUserService {

    private static final String ROLE_PREFIX = "ROLE_";

    // Highest role first: ADMIN > AGENT > REPORTER
    private static final Comparator<UserRole> ROLE_RANK = Comparator.comparingInt(UserRole::ordinal);

    public Jwt getJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new ForbiddenException("Geen geldige authenticatie gevonden");
        }
        return jwt;
    }

    public UUID getCurrentUserId() {
        return UUID.fromString(getJwt().getSubject());
    }

    public UserRole getCurrentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new ForbiddenException("Geen geldige rol gevonden");
        }

        Set<String> knownRoles = Arrays.stream(UserRole.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority != null && authority.startsWith(ROLE_PREFIX))
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .filter(knownRoles::contains)
                .map(UserRole::valueOf)
                .max(ROLE_RANK)
                .orElseThrow(() -> new ForbiddenException("Geen geldige rol gevonden"));
    }

    public boolean isAgentOrAdmin() {
        UserRole role = getCurrentRole();
        return role == UserRole.AGENT || role == UserRole.ADMIN;
    }
}
