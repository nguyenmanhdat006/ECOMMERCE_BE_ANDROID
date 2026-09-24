package com.nguyendat.shopee_be.services;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface FileStorageService {
    
    /**
     * Upload single file
     * @param file MultipartFile to upload
     * @param subDirectory Optional subdirectory within upload directory
     * @return File path/URL
     */
    String uploadFile(MultipartFile file, String subDirectory) throws IOException;
    
    /**
     * Upload multiple files
     * @param files List of MultipartFiles to upload
     * @param subDirectory Optional subdirectory within upload directory
     * @return List of file paths/URLs
     */
    List<String> uploadFiles(List<MultipartFile> files, String subDirectory) throws IOException;
    
    /**
     * Delete file
     * @param fileName Name of file to delete
     * @param subDirectory Optional subdirectory
     * @return true if deleted successfully
     */
    boolean deleteFile(String fileName, String subDirectory);
    
    /**
     * Get file path
     * @param fileName Name of file
     * @param subDirectory Optional subdirectory
     * @return File path
     */
    Path getFilePath(String fileName, String subDirectory);
    
    /**
     * Check if file exists
     * @param fileName Name of file
     * @param subDirectory Optional subdirectory
     * @return true if file exists
     */
    boolean fileExists(String fileName, String subDirectory);
    
    /**
     * Get file URL for access
     * @param fileName Name of file
     * @param subDirectory Optional subdirectory
     * @return File URL
     */
    String getFileUrl(String fileName, String subDirectory);
}
