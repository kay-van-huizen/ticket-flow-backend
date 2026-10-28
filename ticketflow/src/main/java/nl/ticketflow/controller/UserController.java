package nl.ticketflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import nl.ticketflow.dto.response.ErrorResponse;
import nl.ticketflow.dto.response.UserResponse;
import nl.ticketflow.entity.UserRole;
import nl.ticketflow.mapper.UserMapper;
import nl.ticketflow.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Gebruikers", description = "Opvragen van het eigen profiel en van gebruikers per rol")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('REPORTER','AGENT','ADMIN')")
    @Operation(
            summary = "Eigen profiel ophalen",
            description = "Geeft het profiel van de ingelogde gebruiker terug. "
                    + "Bij de eerste aanroep wordt het account aangemaakt op basis van het Keycloak-token; "
                    + "daarna wordt de rol gelijkgetrokken met het token."
    )
    @ApiResponse(responseCode = "200", description = "Profiel van de ingelogde gebruiker")
    @ApiResponse(responseCode = "401", description = "Niet ingelogd of token ongeldig of verlopen",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Geen geldige rol",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(userMapper.toResponse(userService.getOrCreateCurrentUser()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @Operation(
            summary = "Gebruikers per rol ophalen",
            description = "Geeft alle gebruikers met de opgegeven rol terug, bijvoorbeeld om een agent aan een ticket toe te wijzen. "
                    + "Alleen voor agents en beheerders."
    )
    @ApiResponse(responseCode = "200", description = "Lijst van gebruikers met de opgegeven rol")
    @ApiResponse(responseCode = "400", description = "Rol ontbreekt of is ongeldig",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Niet ingelogd of token ongeldig of verlopen",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Alleen agents en beheerders mogen gebruikers opvragen",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<UserResponse>> getUsersByRole(
            @Parameter(description = "Rol om op te filteren", example = "AGENT")
            @RequestParam UserRole role) {
        List<UserResponse> users = userService.findByRole(role).stream()
                .map(userMapper::toResponse)
                .toList();
        return ResponseEntity.ok(users);
    }
}
