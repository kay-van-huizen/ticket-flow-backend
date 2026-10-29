package nl.ticketflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import nl.ticketflow.dto.request.CreateTicketRequest;
import nl.ticketflow.dto.request.UpdateTicketRequest;
import nl.ticketflow.dto.response.ErrorResponse;
import nl.ticketflow.dto.response.TicketResponse;
import nl.ticketflow.entity.TicketPriority;
import nl.ticketflow.entity.TicketStatus;
import nl.ticketflow.service.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets", description = "Aanmaken, bekijken, bijwerken en verwijderen van tickets")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "401", description = "Niet ingelogd of token ongeldig of verlopen",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
@ApiResponse(responseCode = "403", description = "Geen rechten voor deze actie",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('REPORTER','AGENT','ADMIN')")
    @Operation(summary = "Nieuw ticket aanmaken", description = "De ingelogde gebruiker wordt de melder.")
    @ApiResponse(responseCode = "201", description = "Ticket aangemaakt")
    @ApiResponse(responseCode = "400", description = "Validatie mislukt",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Categorie niet gevonden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Categorie is inactief",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request) {
        TicketResponse response = ticketService.createTicket(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('REPORTER','AGENT','ADMIN')")
    @Operation(summary = "Tickets ophalen met optionele filters",
            description = "Een melder ziet alleen eigen tickets. Nieuwste eerst, zonder paginering.")
    @ApiResponse(responseCode = "200", description = "Lijst van tickets")
    @ApiResponse(responseCode = "400", description = "Ongeldige filterwaarde",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<TicketResponse>> getAll(@RequestParam(required = false) TicketStatus status,
                                                       @RequestParam(required = false) TicketPriority priority,
                                                       @RequestParam(required = false) UUID categoryId,
                                                       @RequestParam(required = false) UUID assigneeId) {
        return ResponseEntity.ok(ticketService.listTickets(status, priority, categoryId, assigneeId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('REPORTER','AGENT','ADMIN')")
    @Operation(summary = "Eén ticket ophalen", description = "Een melder kan alleen eigen tickets bekijken.")
    @ApiResponse(responseCode = "200", description = "Het opgevraagde ticket")
    @ApiResponse(responseCode = "404", description = "Ticket niet gevonden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<TicketResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ticketService.getTicket(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('REPORTER','AGENT','ADMIN')")
    @Operation(summary = "Ticket volledig bijwerken",
            description = "Een melder kan alleen een eigen ticket wijzigen zolang de status Open is.")
    @ApiResponse(responseCode = "200", description = "Bijgewerkt ticket")
    @ApiResponse(responseCode = "400", description = "Validatie mislukt",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Ticket of categorie niet gevonden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Categorie is inactief",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<TicketResponse> update(@PathVariable UUID id,
                                                 @Valid @RequestBody UpdateTicketRequest request) {
        return ResponseEntity.ok(ticketService.updateTicket(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ticket verwijderen", description = "Soft delete: het ticket blijft bewaard maar is niet meer zichtbaar.")
    @ApiResponse(responseCode = "204", description = "Ticket verwijderd")
    @ApiResponse(responseCode = "404", description = "Ticket niet gevonden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }
}
