package com.nguyendat.shopee_be.controllers;

import com.nguyendat.shopee_be.dto.FileUploadResponse;
import com.nguyendat.shopee_be.services.FileStorageService;
import com.nguyendat.shopee_be.validation.FileValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
@Tag(name = "File Upload", description = "File upload and management APIs")
public class FileUploadController {
    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);
    private final FileStorageService fileStorageService;
    private final FileValidationService fileValidationService;
    
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload single file", description = "Upload a single file to the server")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @Parameter(description = "File to upload") 
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Subdirectory to store file (optional)") 
            @RequestParam(value = "subDirectory", required = false) String subDirectory,
            @Parameter(description = "Mark as primary file (optional)") 
            @RequestParam(value = "isPrimary", defaultValue = "false") boolean isPrimary) {
        
        try {
            logger.info("Uploading file: " + file.getOriginalFilename());
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(FileUploadResponse.builder()
                                .success(false)
                                .message("File is empty")
                                .build());
            }
            
            logger.info("Validating file: " + file.getOriginalFilename());
            // Validate file
            FileValidationService.ValidationResult validationResult = fileValidationService.validateFile(file);
            if (!validationResult.isValid()) {
                return ResponseEntity.badRequest()
                        .body(FileUploadResponse.builder()
                                .success(false)
                                .message(validationResult.getMessage())
                                .build());
            }
            
            logger.info("Uploading file: " + file.getOriginalFilename());
            String filePath = fileStorageService.uploadFile(file, subDirectory);
            String fileUrl = fileStorageService.getFileUrl(
                    filePath.substring(filePath.lastIndexOf("/") + 1), 
                    subDirectory);
            
            logger.info("File URL: " + fileUrl);
            FileUploadResponse.FileInfo fileInfo = FileUploadResponse.FileInfo.builder()
                    .originalName(file.getOriginalFilename())
                    .fileName(filePath)
                    .fileUrl(fileUrl)
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .isPrimary(isPrimary)
                    .build();
            
            logger.info("File info: " + fileInfo);
            List<FileUploadResponse.FileInfo> files = new ArrayList<>();
            files.add(fileInfo);
            
            return ResponseEntity.ok(FileUploadResponse.builder()
                    .success(true)
                    .message("File uploaded successfully")
                    .files(files)
                    .build());
                    
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(FileUploadResponse.builder()
                            .success(false)
                            .message("Failed to upload file: " + e.getMessage())
                            .build());
        }
    }
    
    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload multiple files", description = "Upload multiple files to the server")
    public ResponseEntity<FileUploadResponse> uploadFiles(
            @Parameter(description = "Files to upload") 
            @RequestParam("files") List<MultipartFile> files,
            @Parameter(description = "Subdirectory to store files (optional)") 
            @RequestParam(value = "subDirectory", required = false) String subDirectory,
            @Parameter(description = "Mark first file as primary (optional)") 
            @RequestParam(value = "markFirstAsPrimary", defaultValue = "false") boolean markFirstAsPrimary) {
        
        try {
            if (files.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(FileUploadResponse.builder()
                                .success(false)
                                .message("No files provided")
                                .build());
            }
            
            // Validate all files
            FileValidationService.ValidationResult validationResult = fileValidationService.validateFiles(files);
            if (!validationResult.isValid()) {
                return ResponseEntity.badRequest()
                        .body(FileUploadResponse.builder()
                                .success(false)
                                .message(validationResult.getMessage())
                                .build());
            }
            
            List<String> filePaths = fileStorageService.uploadFiles(files, subDirectory);
            List<FileUploadResponse.FileInfo> fileInfos = new ArrayList<>();
            
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                String filePath = filePaths.get(i);
                String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
                String fileUrl = fileStorageService.getFileUrl(fileName, subDirectory);
                
                FileUploadResponse.FileInfo fileInfo = FileUploadResponse.FileInfo.builder()
                        .originalName(file.getOriginalFilename())
                        .fileName(filePath)
                        .fileUrl(fileUrl)
                        .fileType(file.getContentType())
                        .fileSize(file.getSize())
                        .isPrimary(markFirstAsPrimary && i == 0)
                        .build();
                
                fileInfos.add(fileInfo);
            }
            
            return ResponseEntity.ok(FileUploadResponse.builder()
                    .success(true)
                    .message("Files uploaded successfully")
                    .files(fileInfos)
                    .build());
                    
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(FileUploadResponse.builder()
                            .success(false)
                            .message("Failed to upload files: " + e.getMessage())
                            .build());
        }
    }
    
    @DeleteMapping("/delete")
    @Operation(summary = "Delete file", description = "Delete a file from the server")
    public ResponseEntity<FileUploadResponse> deleteFile(
            @Parameter(description = "Name of file to delete") 
            @RequestParam("fileName") String fileName,
            @Parameter(description = "Subdirectory of file (optional)") 
            @RequestParam(value = "subDirectory", required = false) String subDirectory) {
        
        try {
            boolean deleted = fileStorageService.deleteFile(fileName, subDirectory);
            
            if (deleted) {
                return ResponseEntity.ok(FileUploadResponse.builder()
                        .success(true)
                        .message("File deleted successfully")
                        .build());
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(FileUploadResponse.builder()
                            .success(false)
                            .message("Failed to delete file: " + e.getMessage())
                            .build());
        }
    }
    
    @GetMapping("/info")
    @Operation(summary = "Get file info", description = "Get information about a file")
    public ResponseEntity<FileUploadResponse> getFileInfo(
            @Parameter(description = "Name of file") 
            @RequestParam("fileName") String fileName,
            @Parameter(description = "Subdirectory of file (optional)") 
            @RequestParam(value = "subDirectory", required = false) String subDirectory) {
        
        try {
            boolean exists = fileStorageService.fileExists(fileName, subDirectory);
            
            if (exists) {
                String fileUrl = fileStorageService.getFileUrl(fileName, subDirectory);
                
                FileUploadResponse.FileInfo fileInfo = FileUploadResponse.FileInfo.builder()
                        .fileName(fileName)
                        .fileUrl(fileUrl)
                        .build();
                
                List<FileUploadResponse.FileInfo> files = new ArrayList<>();
                files.add(fileInfo);
                
                return ResponseEntity.ok(FileUploadResponse.builder()
                        .success(true)
                        .message("File found")
                        .files(files)
                        .build());
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(FileUploadResponse.builder()
                            .success(false)
                            .message("Failed to get file info: " + e.getMessage())
                            .build());
        }
    }
    
}
