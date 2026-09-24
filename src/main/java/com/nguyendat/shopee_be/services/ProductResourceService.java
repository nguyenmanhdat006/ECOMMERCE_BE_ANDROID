package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.dto.FileUploadResponse;
import com.nguyendat.shopee_be.dto.ProductResourceDto;
import com.nguyendat.shopee_be.entities.Product;
import com.nguyendat.shopee_be.entities.Resources;
import com.nguyendat.shopee_be.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductResourceService {
    
    private final FileStorageService fileStorageService;
    private final ProductRepository productRepository;
    
    /**
     * Upload product images and create resource records
     */
    public FileUploadResponse uploadProductResources(
            UUID productId, 
            List<MultipartFile> files, 
            boolean markFirstAsPrimary) throws IOException {
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        // Upload files
        List<String> filePaths = fileStorageService.uploadFiles(files, "products/" + productId);
        
        // Create resource entities
        List<Resources> resources = new java.util.ArrayList<>();
        List<FileUploadResponse.FileInfo> fileInfos = new java.util.ArrayList<>();
        
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String filePath = filePaths.get(i);
            String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
            String fileUrl = fileStorageService.getFileUrl(fileName, "products/" + productId);
            
            // Create resource entity
            Resources resource = Resources.builder()
                    .name(file.getOriginalFilename())
                    .url(fileUrl)
                    .type(file.getContentType())
                    .isPrimary(markFirstAsPrimary && i == 0)
                    .product(product)
                    .build();
            
            resources.add(resource);
            
            // Create file info for response
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
        
        // Save resources to database
        product.getResources().addAll(resources);
        productRepository.save(product);
        
        return FileUploadResponse.builder()
                .success(true)
                .message("Product resources uploaded successfully")
                .files(fileInfos)
                .build();
    }
    
    /**
     * Get product resources
     */
    public List<ProductResourceDto> getProductResources(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        return product.getResources().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Delete product resource
     */
    public boolean deleteProductResource(UUID productId, UUID resourceId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        Resources resource = product.getResources().stream()
                .filter(r -> r.getId().equals(resourceId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Resource not found"));
        
        // Delete file from storage
        String fileName = resource.getUrl().substring(resource.getUrl().lastIndexOf("/") + 1);
        fileStorageService.deleteFile(fileName, "products/" + productId);
        
        // Remove from database
        product.getResources().remove(resource);
        productRepository.save(product);
        
        return true;
    }
    
    /**
     * Set primary resource
     */
    public boolean setPrimaryResource(UUID productId, UUID resourceId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        // Reset all resources to non-primary
        product.getResources().forEach(r -> r.setIsPrimary(false));
        
        // Set selected resource as primary
        Resources primaryResource = product.getResources().stream()
                .filter(r -> r.getId().equals(resourceId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Resource not found"));
        
        primaryResource.setIsPrimary(true);
        productRepository.save(product);
        
        return true;
    }
    
    private ProductResourceDto mapToDto(Resources resource) {
        return ProductResourceDto.builder()
                .id(resource.getId())
                .name(resource.getName())
                .url(resource.getUrl())
                .type(resource.getType())
                .isPrimary(resource.getIsPrimary())
                .build();
    }
}
