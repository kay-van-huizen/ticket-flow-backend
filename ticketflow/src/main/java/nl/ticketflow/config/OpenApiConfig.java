package nl.ticketflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI ticketFlowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TicketFlow API")
                        .description("REST API voor het melden, toewijzen en afhandelen van tickets. "
                                + "Log in via Keycloak, klik op Authorize en plak je access token.")
                        .version("1.0"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Plak hier alleen het access token uit Keycloak, zonder het woord \"Bearer\".")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
