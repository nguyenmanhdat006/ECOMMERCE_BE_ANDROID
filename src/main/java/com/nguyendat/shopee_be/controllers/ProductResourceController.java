package com.nguyendat.shopee_be.controllers;

import com.nguyendat.shopee_be.dto.FileUploadResponse;
import com.nguyendat.shopee_be.dto.ProductResourceDto;
import com.nguyendat.shopee_be.services.ProductResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product Resources", description = "Product image and resource management APIs")
public class ProductResourceController {
    
    private final ProductResourceService productResourceService;
    
    @PostMapping(value = "/{productId}/resources", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload product resources", description = "Upload images and resources for a product")
    public ResponseEntity<FileUploadResponse> uploadProductResources(
            @Parameter(description = "Product ID") 
            @PathVariable UUID productId,
            @Parameter(description = "Files to upload") 
            @RequestParam("files") List<MultipartFile> files,
            @Parameter(description = "Mark first file as primary") 
            @RequestParam(value = "markFirstAsPrimary", defaultValue = "true") boolean markFirstAsPrimary) {
        
        try {
            if (files.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(FileUploadResponse.builder()
                                .success(false)
                                .message("No files provided")
                                .build());
            }
            
            FileUploadResponse response = productResourceService.uploadProductResources(
                    productId, files, markFirstAsPrimary);
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(FileUploadResponse.builder()
                            .success(false)
                            .message("Failed to upload product resources: " + e.getMessage())
                            .build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(FileUploadResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }
    
    @GetMapping("/{productId}/resources")
    @Operation(summary = "Get product resources", description = "Get all resources for a product")
    public ResponseEntity<List<ProductResourceDto>> getProductResources(
            @Parameter(description = "Product ID") 
            @PathVariable UUID productId) {
        
        try {
            List<ProductResourceDto> resources = productResourceService.getProductResources(productId);
            return ResponseEntity.ok(resources);
            
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{productId}/resources/{resourceId}")
    @Operation(summary = "Delete product resource", description = "Delete a specific resource from a product")
    public ResponseEntity<FileUploadResponse> deleteProductResource(
            @Parameter(description = "Product ID") 
            @PathVariable UUID productId,
            @Parameter(description = "Resource ID") 
            @PathVariable UUID resourceId) {
        
        try {
            boolean deleted = productResourceService.deleteProductResource(productId, resourceId);
            
            if (deleted) {
                return ResponseEntity.ok(FileUploadResponse.builder()
                        .success(true)
                        .message("Product resource deleted successfully")
                        .build());
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(FileUploadResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }
    
    @PutMapping("/{productId}/resources/{resourceId}/primary")
    @Operation(summary = "Set primary resource", description = "Set a resource as the primary image for a product")
    public ResponseEntity<FileUploadResponse> setPrimaryResource(
            @Parameter(description = "Product ID") 
            @PathVariable UUID productId,
            @Parameter(description = "Resource ID") 
            @PathVariable UUID resourceId) {
        
        try {
            boolean updated = productResourceService.setPrimaryResource(productId, resourceId);
            
            if (updated) {
                return ResponseEntity.ok(FileUploadResponse.builder()
                        .success(true)
                        .message("Primary resource updated successfully")
                        .build());
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(FileUploadResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }
}
