package com.nguyendat.shopee_be.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUploadResponse {
    
    private String message;
    private boolean success;
    private List<FileInfo> files;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileInfo {
        private String originalName;
        private String fileName;
        private String fileUrl;
        private String fileType;
        private long fileSize;
        private boolean isPrimary;
    }
}
