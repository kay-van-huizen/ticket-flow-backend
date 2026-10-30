package nl.ticketflow.integration;

import nl.ticketflow.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@ActiveProfiles("test")
abstract class BaseIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    protected JsonMapper jsonMapper;

    protected MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    /** A logged-in test user with its own subject, so tests never share data. */
    protected record TestUser(UUID id, String username, UserRole role) {

        static TestUser random(UserRole role) {
            UUID id = UUID.randomUUID();
            return new TestUser(id, role.name().toLowerCase() + "-" + id, role);
        }
    }

    // Authorities with the ROLE_ prefix, exactly as KeycloakRoleConverter produces them and CurrentUserService reads them
    protected static JwtRequestPostProcessor jwtFor(TestUser user) {
        return jwt()
                .jwt(token -> token
                        .subject(user.id().toString())
                        .claim("preferred_username", user.username())
                        .claim("email", user.username() + "@example.com")
                        .claim("name", "Test " + user.username()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + user.role().name()));
    }
}
