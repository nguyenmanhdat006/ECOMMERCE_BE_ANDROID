# Upload API Architecture

## Kiến trúc tổng quan

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER                            │
├─────────────────────────────────────────────────────────────────┤
│  Frontend (React/Vue/Angular)  │  Mobile App  │  Postman/curl  │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                      API LAYER                                 │
├─────────────────────────────────────────────────────────────────┤
│  FileUploadController  │  ProductResourceController  │  StaticFileController │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                    SERVICE LAYER                               │
├─────────────────────────────────────────────────────────────────┤
│  FileValidationService  │  ProductResourceService  │  FileStorageService │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                   STORAGE LAYER                                 │
├─────────────────────────────────────────────────────────────────┤
│  LocalFileStorageService  │  S3FileStorageService  │  Database │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                   STORAGE BACKEND                              │
├─────────────────────────────────────────────────────────────────┤
│  Local File System  │  AWS S3  │  PostgreSQL (Resources)     │
└─────────────────────────────────────────────────────────────────┘
```

## Luồng xử lý Upload

### 1. Upload Single File
```
Client → FileUploadController → FileValidationService → FileStorageService → Storage Backend
```

### 2. Upload Product Resources
```
Client → ProductResourceController → ProductResourceService → FileStorageService → Database + Storage Backend
```

### 3. File Access
```
Client → StaticFileController → LocalFileStorageService → Local File System
Client → S3FileStorageService → AWS S3 → Presigned URL
```

## Cấu hình Storage

### Local Storage Flow
```
Request → Validation → LocalFileStorageService → ./uploads/ → StaticFileController → Response
```

### S3 Storage Flow
```
Request → Validation → S3FileStorageService → AWS S3 → Direct URL → Response
```

## Security & Validation

### File Validation Pipeline
```
1. File Size Check (10MB max)
2. File Type Check (images, documents)
3. File Extension Check
4. Malicious Content Check
5. Upload to Storage
6. Database Record Creation
```

### Security Features
- File type whitelist
- File size limits
- Malicious filename detection
- Content type validation
- Path traversal protection

## API Endpoints Summary

### File Management
- `POST /api/files/upload` - Upload single file
- `POST /api/files/upload-multiple` - Upload multiple files
- `DELETE /api/files/delete` - Delete file
- `GET /api/files/info` - Get file info

### Product Resources
- `POST /api/products/{id}/resources` - Upload product images
- `GET /api/products/{id}/resources` - Get product resources
- `DELETE /api/products/{id}/resources/{resourceId}` - Delete resource
- `PUT /api/products/{id}/resources/{resourceId}/primary` - Set primary

### Static Access
- `GET /files/**` - Access uploaded files

## Configuration Options

### Environment Variables
```bash
# Storage Type
FILE_STORAGE_TYPE=local|s3

# Local Storage
file.upload-dir=./uploads
file.upload-url=http://localhost:8080/files

# AWS S3
AWS_S3_BUCKET_NAME=your-bucket
AWS_S3_REGION=us-east-1
AWS_S3_ACCESS_KEY=your-key
AWS_S3_SECRET_KEY=your-secret
```

### File Limits
- Max file size: 10MB
- Max files per upload: 10
- Allowed types: jpg, jpeg, png, gif, webp, pdf, doc, docx, xls, xlsx

## Database Schema

### Resources Table
```sql
CREATE TABLE product_resources (
    id UUID PRIMARY KEY,
    name VARCHAR NOT NULL,
    url VARCHAR NOT NULL,
    is_primary BOOLEAN NOT NULL,
    type VARCHAR NOT NULL,
    product_id UUID REFERENCES products(id)
);
```

## Error Handling

### Common Errors
- `400 Bad Request` - Invalid file type/size
- `404 Not Found` - File not found
- `500 Internal Server Error` - Storage failure
- `413 Payload Too Large` - File too big

### Error Response Format
```json
{
  "success": false,
  "message": "Error description",
  "files": []
}
```

## Performance Considerations

### Local Storage
- ✅ Fast access
- ✅ No external dependencies
- ❌ Limited scalability
- ❌ Single point of failure

### S3 Storage
- ✅ Highly scalable
- ✅ Global CDN support
- ✅ Automatic backup
- ❌ Network dependency
- ❌ Additional costs

## Monitoring & Logging

### Key Metrics
- Upload success rate
- File size distribution
- Storage usage
- Error rates
- Response times

### Logging Points
- File validation results
- Upload attempts
- Storage operations
- Error conditions
- Performance metrics
