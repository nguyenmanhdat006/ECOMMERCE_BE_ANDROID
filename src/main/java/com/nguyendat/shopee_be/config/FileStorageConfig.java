package com.nguyendat.shopee_be.config;

import com.nguyendat.shopee_be.services.FileStorageService;
import com.nguyendat.shopee_be.services.LocalFileStorageService;
import com.nguyendat.shopee_be.services.S3FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class FileStorageConfig {
    
    @Value("${file.storage.type:local}")
    private String storageType;
    
    @Autowired(required = false)
    private S3FileStorageService s3FileStorageService;
    
    @Bean
    @Primary
    public FileStorageService fileStorageService(LocalFileStorageService localFileStorageService) {
        if ("s3".equalsIgnoreCase(storageType) && s3FileStorageService != null) {
            return s3FileStorageService;
        }
        return localFileStorageService;
    }
}
