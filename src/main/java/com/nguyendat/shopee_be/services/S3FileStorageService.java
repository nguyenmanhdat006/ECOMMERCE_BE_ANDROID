package com.nguyendat.shopee_be.services;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "file.storage.type", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {
    
    private final S3Client s3Client;
    
    @Value("${aws.s3.bucket-name}")
    private String bucketName;
    
    @Value("${aws.s3.region}")
    private String region;
    
    public S3FileStorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }
    
    @Override
    public String uploadFile(MultipartFile file, String subDirectory) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + "." + extension;
        
        // Create S3 key
        String s3Key = subDirectory != null ? 
            subDirectory + "/" + uniqueFilename : uniqueFilename;
        
        try {
            // Upload to S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();
            
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(
                    file.getInputStream(), file.getSize()));
            
            return s3Key;
            
        } catch (S3Exception e) {
            throw new IOException("Failed to upload file to S3: " + e.getMessage(), e);
        }
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
            String s3Key = subDirectory != null ? 
                subDirectory + "/" + fileName : fileName;
            
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            
            s3Client.deleteObject(deleteObjectRequest);
            return true;
            
        } catch (S3Exception e) {
            return false;
        }
    }
    
    @Override
    public Path getFilePath(String fileName, String subDirectory) {
        // For S3, we return a virtual path since files are stored in cloud
        String s3Key = subDirectory != null ? 
            subDirectory + "/" + fileName : fileName;
        return Paths.get("s3://" + bucketName + "/" + s3Key);
    }
    
    @Override
    public boolean fileExists(String fileName, String subDirectory) {
        try {
            String s3Key = subDirectory != null ? 
                subDirectory + "/" + fileName : fileName;
            
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            
            s3Client.headObject(headObjectRequest);
            return true;
            
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            return false;
        }
    }
    
    @Override
    public String getFileUrl(String fileName, String subDirectory) {
        String s3Key = subDirectory != null ? 
            subDirectory + "/" + fileName : fileName;
        
        // Generate direct S3 URL
        return String.format("https://%s.s3.%s.amazonaws.com/%s", 
                bucketName, region, s3Key);
    }
}
