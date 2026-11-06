package nl.ticketflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import nl.ticketflow.dto.response.AttachmentDownload;
import nl.ticketflow.dto.response.AttachmentResponse;
import nl.ticketflow.dto.response.ErrorResponse;
import nl.ticketflow.service.AttachmentService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments/{commentId}/attachment")
@PreAuthorize("hasAnyRole('REPORTER','AGENT','ADMIN')")
@Tag(name = "Bijlagen", description = "Uploaden, downloaden en verwijderen van de bijlage bij een reactie")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "401", description = "Niet ingelogd of token ongeldig of verlopen",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
@ApiResponse(responseCode = "403", description = "Geen rechten voor deze actie",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Bijlage uploaden",
            description = "Eén bijlage per reactie, in het formulierveld 'file'. Toegestaan: pdf, png, jpg, txt "
                    + "en log, maximaal 10 MB. Alleen de auteur van de reactie of een beheerder mag dit.")
    @ApiResponse(responseCode = "201", description = "Bijlage geüpload")
    @ApiResponse(responseCode = "400", description = "Bestand ontbreekt, is leeg of het bestandstype is niet toegestaan",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Ticket of reactie niet gevonden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Ticket is gesloten of de reactie heeft al een bijlage",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "413", description = "Bestand is groter dan 10 MB",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<AttachmentResponse> upload(@PathVariable UUID ticketId,
                                                     @PathVariable UUID commentId,
                                                     @RequestPart("file") MultipartFile file) {
        AttachmentResponse response = attachmentService.upload(ticketId, commentId, file);
        // The attachment is a singleton under its comment, so it is downloaded from this same URL
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "Bijlage downloaden",
            description = "Geeft het bestand terug met de oorspronkelijke bestandsnaam. Een melder kan alleen "
                    + "bijlagen van niet-interne reacties op een eigen ticket downloaden.")
    @ApiResponse(responseCode = "200", description = "Het bestand")
    @ApiResponse(responseCode = "404", description = "Ticket, reactie of bijlage niet gevonden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Resource> download(@PathVariable UUID ticketId, @PathVariable UUID commentId) {
        AttachmentDownload download = attachmentService.download(ticketId, commentId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(download.resource());
    }

    @DeleteMapping
    @Operation(summary = "Bijlage verwijderen", description = "Alleen de auteur van de reactie of een beheerder mag dit.")
    @ApiResponse(responseCode = "204", description = "Bijlage verwijderd")
    @ApiResponse(responseCode = "404", description = "Ticket, reactie of bijlage niet gevonden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Ticket is gesloten",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> delete(@PathVariable UUID ticketId, @PathVariable UUID commentId) {
        attachmentService.delete(ticketId, commentId);
        return ResponseEntity.noContent().build();
    }
}
