package nl.ticketflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import nl.ticketflow.dto.request.ChangeCategoryStatusRequest;
import nl.ticketflow.dto.request.CreateCategoryRequest;
import nl.ticketflow.dto.request.UpdateCategoryRequest;
import nl.ticketflow.dto.response.CategoryResponse;
import nl.ticketflow.dto.response.ErrorResponse;
import nl.ticketflow.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/categories")
@Tag(name = "Categorieën", description = "Beheer van hoofd- en subcategorieën")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "401", description = "Niet ingelogd of token ongeldig of verlopen",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
@ApiResponse(responseCode = "403", description = "Geen rechten voor deze actie",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('REPORTER','AGENT','ADMIN')")
    @Operation(summary = "Alle categorieën ophalen")
    @ApiResponse(responseCode = "200", description = "Lijst van alle categorieën")
    public ResponseEntity<List<CategoryResponse>> getAll() {
        return ResponseEntity.ok(categoryService.getAll());
    }

    @GetMapping("/roots")
    @PreAuthorize("hasAnyRole('REPORTER','AGENT','ADMIN')")
    @Operation(summary = "Alle hoofdcategorieën ophalen")
    @ApiResponse(responseCode = "200", description = "Lijst van hoofdcategorieën")
    public ResponseEntity<List<CategoryResponse>> getRoots() {
        return ResponseEntity.ok(categoryService.getRoots());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('REPORTER','AGENT','ADMIN')")
    @Operation(summary = "Eén categorie ophalen")
    @ApiResponse(responseCode = "200", description = "De opgevraagde categorie")
    @ApiResponse(responseCode = "404", description = "Categorie niet gevonden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<CategoryResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryService.getById(id));
    }

    @GetMapping("/{id}/children")
    @PreAuthorize("hasAnyRole('REPORTER','AGENT','ADMIN')")
    @Operation(summary = "Subcategorieën van een categorie ophalen")
    @ApiResponse(responseCode = "200", description = "Lijst van directe subcategorieën")
    @ApiResponse(responseCode = "404", description = "Categorie niet gevonden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<CategoryResponse>> getChildren(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryService.getChildren(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @Operation(summary = "Nieuwe categorie aanmaken", description = "Met een parentId wordt het een subcategorie.")
    @ApiResponse(responseCode = "201", description = "Categorie aangemaakt")
    @ApiResponse(responseCode = "400", description = "Validatie mislukt",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Bovenliggende categorie niet gevonden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Bovenliggende categorie is inactief",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse response = categoryService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @Operation(summary = "Categorie volledig bijwerken", description = "Zonder parentId wordt het een hoofdcategorie.")
    @ApiResponse(responseCode = "200", description = "Bijgewerkte categorie")
    @ApiResponse(responseCode = "400", description = "Validatie mislukt",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Categorie of bovenliggende categorie niet gevonden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Ongeldige bovenliggende categorie",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<CategoryResponse> update(@PathVariable UUID id,
                                                   @Valid @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @Operation(summary = "Status van een categorie wijzigen")
    @ApiResponse(responseCode = "200", description = "Categorie met de nieuwe status")
    @ApiResponse(responseCode = "400", description = "Status ontbreekt of is ongeldig",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Categorie niet gevonden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<CategoryResponse> changeStatus(@PathVariable UUID id,
                                                         @Valid @RequestBody ChangeCategoryStatusRequest request) {
        return ResponseEntity.ok(categoryService.changeStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Categorie verwijderen")
    @ApiResponse(responseCode = "204", description = "Categorie verwijderd")
    @ApiResponse(responseCode = "404", description = "Categorie niet gevonden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Categorie heeft nog subcategorieën of gekoppelde tickets",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
