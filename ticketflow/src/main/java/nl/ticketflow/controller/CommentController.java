package nl.ticketflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import nl.ticketflow.dto.request.CreateCommentRequest;
import nl.ticketflow.dto.request.UpdateCommentRequest;
import nl.ticketflow.dto.response.CommentResponse;
import nl.ticketflow.dto.response.ErrorResponse;
import nl.ticketflow.service.CommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")

@PreAuthorize("hasAnyRole('REPORTER','AGENT','ADMIN')")
@Tag(name = "Reacties", description = "Plaatsen, bekijken, wijzigen en verwijderen van reacties op een ticket")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "401", description = "Niet ingelogd of token ongeldig of verlopen",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
@ApiResponse(responseCode = "403", description = "Geen rechten voor deze actie",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    @Operation(summary = "Reacties van een ticket ophalen",
            description = "Oudste eerst. Een melder ziet alleen de niet-interne reacties van een eigen ticket.")
    @ApiResponse(responseCode = "200", description = "Lijst van reacties")
    @ApiResponse(responseCode = "404", description = "Ticket niet gevonden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<CommentResponse>> getAll(@PathVariable UUID ticketId) {
        return ResponseEntity.ok(commentService.list(ticketId));
    }

    @PostMapping
    @Operation(summary = "Reactie plaatsen",
            description = "De ingelogde gebruiker wordt de auteur. Alleen agents en beheerders mogen interne "
                    + "reacties plaatsen.")
    @ApiResponse(responseCode = "201", description = "Reactie geplaatst")
    @ApiResponse(responseCode = "400", description = "Validatie mislukt",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Ticket niet gevonden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Ticket is gesloten",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<CommentResponse> create(@PathVariable UUID ticketId,
                                                  @Valid @RequestBody CreateCommentRequest request) {
        CommentResponse response = commentService.create(ticketId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{commentId}")
    @Operation(summary = "Reactie wijzigen",
            description = "Alleen de tekst kan worden gewijzigd. Alleen de auteur of een beheerder mag dit.")
    @ApiResponse(responseCode = "200", description = "Gewijzigde reactie")
    @ApiResponse(responseCode = "400", description = "Validatie mislukt",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Ticket of reactie niet gevonden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<CommentResponse> update(@PathVariable UUID ticketId,
                                                  @PathVariable UUID commentId,
                                                  @Valid @RequestBody UpdateCommentRequest request) {
        return ResponseEntity.ok(commentService.update(ticketId, commentId, request));
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "Reactie verwijderen", description = "Alleen de auteur of een beheerder mag dit.")
    @ApiResponse(responseCode = "204", description = "Reactie verwijderd")
    @ApiResponse(responseCode = "404", description = "Ticket of reactie niet gevonden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> delete(@PathVariable UUID ticketId, @PathVariable UUID commentId) {
        commentService.delete(ticketId, commentId);
        return ResponseEntity.noContent().build();
    }
}
