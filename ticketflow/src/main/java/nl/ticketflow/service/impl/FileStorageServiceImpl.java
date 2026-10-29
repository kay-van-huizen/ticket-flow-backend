package nl.ticketflow.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import nl.ticketflow.exception.BusinessRuleException;
import nl.ticketflow.exception.ResourceNotFoundException;
import nl.ticketflow.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    private static final Map<String, Set<String>> ALLOWED_TYPES = Map.of(
            "pdf", Set.of("application/pdf"),
            "png", Set.of("image/png"),
            "jpg", Set.of("image/jpeg"),
            "jpeg", Set.of("image/jpeg"),
            "txt", Set.of("text/plain"),
            "log", Set.of("text/plain", "application/octet-stream")
    );

    private static final String TYPE_NOT_ALLOWED = "Bestandstype niet toegestaan. Toegestaan: pdf, png, jpg, txt, log";

    private final Path root;

    public FileStorageServiceImpl(@Value("${app.storage.path}") String storagePath) {
        this.root = Paths.get(storagePath).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(root);
        log.info("File storage directory: {}", root);
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Het bestand is leeg");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            // Same exception as Spring's multipart limit, so GlobalExceptionHandler returns 413
            throw new MaxUploadSizeExceededException(MAX_SIZE_BYTES);
        }
        String extension = allowedExtension(file);

        // The client file name is never used on disk; only the whitelisted extension is kept
        String storedName = UUID.randomUUID() + "." + extension;
        Path target = resolveInsideRoot(storedName);
        if (target == null) {
            throw new BusinessRuleException(TYPE_NOT_ALLOWED);
        }

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target);
        } catch (IOException e) {
            throw new UncheckedIOException("Het bestand kon niet worden opgeslagen", e);
        }
        return storedName;
    }

    @Override
    public Resource load(String storagePath) {
        Path file = resolveInsideRoot(storagePath);
        if (file == null || !Files.isRegularFile(file) || !Files.isReadable(file)) {
            throw new ResourceNotFoundException("Bestand is niet gevonden");
        }
        return new FileSystemResource(file);
    }

    @Override
    public void delete(String storagePath) {
        Path file = resolveInsideRoot(storagePath);
        if (file == null) {
            log.warn("Refused to delete file outside storage directory: {}", storagePath);
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Could not delete file {}: {}", file, e.getMessage());
        }
    }

    private String allowedExtension(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (!StringUtils.hasText(originalName)) {
            throw new BusinessRuleException(TYPE_NOT_ALLOWED);
        }
        String extension = StringUtils.getFilenameExtension(originalName);
        String contentType = file.getContentType();
        if (extension == null || contentType == null) {
            throw new BusinessRuleException(TYPE_NOT_ALLOWED);
        }

        extension = extension.toLowerCase(Locale.ROOT);
        // Strip parameters such as ";charset=UTF-8"
        contentType = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        Set<String> allowedContentTypes = ALLOWED_TYPES.get(extension);
        if (allowedContentTypes == null || !allowedContentTypes.contains(contentType)) {
            throw new BusinessRuleException(TYPE_NOT_ALLOWED);
        }
        return extension;
    }

    // Returns null when the path is blank or would end up outside the storage directory (path traversal)
    private Path resolveInsideRoot(String storagePath) {
        if (!StringUtils.hasText(storagePath)) {
            return null;
        }
        Path resolved = root.resolve(storagePath).normalize();
        return root.equals(resolved.getParent()) ? resolved : null;
    }
}
