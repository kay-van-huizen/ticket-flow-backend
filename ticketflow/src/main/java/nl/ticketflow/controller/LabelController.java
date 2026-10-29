package nl.ticketflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import nl.ticketflow.dto.request.CreateLabelRequest;
import nl.ticketflow.dto.response.ErrorResponse;
import nl.ticketflow.dto.response.LabelResponse;
import nl.ticketflow.service.LabelService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/labels")
@Tag(name = "Labels", description = "Beheer van labels om tickets mee te markeren")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "401", description = "Niet ingelogd of token ongeldig of verlopen",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
@ApiResponse(responseCode = "403", description = "Geen rechten voor deze actie",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class LabelController {

    private final LabelService labelService;

    public LabelController(LabelService labelService) {
        this.labelService = labelService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('REPORTER','AGENT','ADMIN')")
    @Operation(summary = "Alle labels ophalen", description = "Alfabetisch gesorteerd op naam.")
    @ApiResponse(responseCode = "200", description = "Lijst van alle labels")
    public ResponseEntity<List<LabelResponse>> getAll() {
        return ResponseEntity.ok(labelService.getAll());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @Operation(summary = "Nieuw label aanmaken", description = "De naam moet uniek zijn, ongeacht hoofdletters.")
    @ApiResponse(responseCode = "201", description = "Label aangemaakt")
    @ApiResponse(responseCode = "400", description = "Validatie mislukt",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Er bestaat al een label met deze naam",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<LabelResponse> create(@Valid @RequestBody CreateLabelRequest request) {
        LabelResponse response = labelService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Label verwijderen", description = "Alleen mogelijk als het label aan geen enkel ticket gekoppeld is.")
    @ApiResponse(responseCode = "204", description = "Label verwijderd")
    @ApiResponse(responseCode = "404", description = "Label niet gevonden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Label is nog aan tickets gekoppeld",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        labelService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
