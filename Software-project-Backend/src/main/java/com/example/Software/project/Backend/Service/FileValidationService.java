package com.example.Software.project.Backend.Service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * Validates uploaded Excel files before they reach Apache POI parsing.
 * Extension + declared content-type are checked here as a first line of defense;
 * POI itself will reject anything that isn't actually a valid OOXML/OLE2 workbook.
 */
@Service
public class FileValidationService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5 MB

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".xlsx", ".xls");

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx
            "application/vnd.ms-excel", // .xls
            "application/octet-stream" // some browsers/clients send this generic type for .xlsx
    );

    public void validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file uploaded, or the uploaded file is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "File exceeds the maximum allowed size of " + (MAX_FILE_SIZE_BYTES / (1024 * 1024)) + "MB");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Uploaded file has no filename");
        }
        String lowerName = filename.toLowerCase();
        boolean hasAllowedExtension = ALLOWED_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        if (!hasAllowedExtension) {
            throw new IllegalArgumentException("Only .xlsx or .xls files are allowed");
        }

        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported file content-type: " + contentType);
        }
    }
}
