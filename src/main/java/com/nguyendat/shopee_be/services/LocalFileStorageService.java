package com.nguyendat.shopee_be.services;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {
    
    @Value("${file.upload-dir}")
    private String uploadDir;
    
    @Value("${file.upload-url}")
    private String uploadUrl;
    
    @Override
    public String uploadFile(MultipartFile file, String subDirectory) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        // Create directory if not exists
        Path uploadPath = createUploadPath(subDirectory);
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + "." + extension;
        
        // Save file
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Return relative path for URL construction
        String relativePath = subDirectory != null ? 
            subDirectory + "/" + uniqueFilename : uniqueFilename;
        
        return relativePath;
    }
    
    @Override
    public List<String> uploadFiles(List<MultipartFile> files, String subDirectory) throws IOException {
        List<String> uploadedFiles = new ArrayList<>();
        
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String filePath = uploadFile(file, subDirectory);
                uploadedFiles.add(filePath);
            }
        }
        
        return uploadedFiles;
    }
    
    @Override
    public boolean deleteFile(String fileName, String subDirectory) {
        try {
            Path filePath = getFilePath(fileName, subDirectory);
            return Files.deleteIfExists(filePath);
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public Path getFilePath(String fileName, String subDirectory) {
        try {
            Path uploadPath = createUploadPath(subDirectory);
            return uploadPath.resolve(fileName);
        } catch (IOException e) {
            // Fallback to default upload directory
            return Paths.get(uploadDir, fileName);
        }
    }
    
    @Override
    public boolean fileExists(String fileName, String subDirectory) {
        try {
            Path filePath = getFilePath(fileName, subDirectory);
            return Files.exists(filePath);
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String getFileUrl(String fileName, String subDirectory) {
        String relativePath = subDirectory != null ? 
            subDirectory + "/" + fileName : fileName;
        return uploadUrl + "/" + relativePath;
    }
    
    private Path createUploadPath(String subDirectory) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        
        if (subDirectory != null && !subDirectory.isEmpty()) {
            uploadPath = uploadPath.resolve(subDirectory);
        }
        
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        return uploadPath;
    }
}
