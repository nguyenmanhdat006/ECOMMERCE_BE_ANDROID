package com.nguyendat.shopee_be.validation;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Service
public class FileValidationService {
    
    // Allowed file types
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );
    
    private static final List<String> ALLOWED_DOCUMENT_TYPES = Arrays.asList(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );
    
    // File size limits (in bytes)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    
    // Allowed file extensions
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "pdf", "doc", "docx", "xls", "xlsx"
    );
    
    /**
     * Validate single file
     */
    public ValidationResult validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return new ValidationResult(false, "File is empty or null");
        }
        
        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            return new ValidationResult(false, "File size exceeds maximum limit of 10MB");
        }
        
        // Check content type
        String contentType = file.getContentType();
        if (contentType == null) {
            return new ValidationResult(false, "Cannot determine file type");
        }
        
        // Check if content type is allowed
        if (!isAllowedContentType(contentType)) {
            return new ValidationResult(false, "File type not allowed. Allowed types: " + 
                    String.join(", ", ALLOWED_EXTENSIONS));
        }
        
        // Check file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            return new ValidationResult(false, "File name is empty");
        }
        
        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            return new ValidationResult(false, "File extension not allowed. Allowed extensions: " + 
                    String.join(", ", ALLOWED_EXTENSIONS));
        }
        
        // Check for malicious file names
        if (containsMaliciousContent(originalFilename)) {
            return new ValidationResult(false, "File name contains potentially malicious content");
        }
        
        return new ValidationResult(true, "File is valid");
    }
    
    /**
     * Validate multiple files
     */
    public ValidationResult validateFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return new ValidationResult(false, "No files provided");
        }
        
        if (files.size() > 10) {
            return new ValidationResult(false, "Maximum 10 files allowed per upload");
        }
        
        for (MultipartFile file : files) {
            ValidationResult result = validateFile(file);
            if (!result.isValid()) {
                return result;
            }
        }
        
        return new ValidationResult(true, "All files are valid");
    }
    
    /**
     * Check if content type is allowed
     */
    private boolean isAllowedContentType(String contentType) {
        return ALLOWED_IMAGE_TYPES.contains(contentType) || 
               ALLOWED_DOCUMENT_TYPES.contains(contentType);
    }
    
    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
    
    /**
     * Check for malicious content in filename
     */
    private boolean containsMaliciousContent(String filename) {
        String[] maliciousPatterns = {
                "..", "/", "\\", "<", ">", ":", "\"", "|", "?", "*",
                "script", "javascript", "vbscript", "onload", "onerror"
        };
        
        String lowerFilename = filename.toLowerCase();
        for (String pattern : maliciousPatterns) {
            if (lowerFilename.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Validation result class
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
