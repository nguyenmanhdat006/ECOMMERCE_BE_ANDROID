# Upload API Implementation Guide

## Tổng quan

Dự án đã được triển khai Upload API với 2 phương thức lưu trữ:
1. **Local Storage** - Lưu file trực tiếp trên server (mặc định)
2. **AWS S3** - Lưu file trên cloud storage

## Cấu hình

### 1. Local Storage (Mặc định)
```properties
# application.properties
file.storage.type=local
file.upload-dir=./uploads
file.upload-url=http://localhost:8080/files
```

### 2. AWS S3 Storage
```properties
# application.properties
file.storage.type=s3
aws.s3.bucket-name=your-bucket-name
aws.s3.region=us-east-1
aws.s3.access-key=your-access-key
aws.s3.secret-key=your-secret-key
```

## API Endpoints

### 1. File Upload APIs

#### Upload Single File
```http
POST /api/files/upload
Content-Type: multipart/form-data

Parameters:
- file: MultipartFile (required)
- subDirectory: String (optional)
- isPrimary: boolean (optional, default: false)
```

#### Upload Multiple Files
```http
POST /api/files/upload-multiple
Content-Type: multipart/form-data

Parameters:
- files: List<MultipartFile> (required)
- subDirectory: String (optional)
- markFirstAsPrimary: boolean (optional, default: false)
```

#### Delete File
```http
DELETE /api/files/delete?fileName={fileName}&subDirectory={subDirectory}
```

#### Get File Info
```http
GET /api/files/info?fileName={fileName}&subDirectory={subDirectory}
```

### 2. Product Resource APIs

#### Upload Product Images
```http
POST /api/products/{productId}/resources
Content-Type: multipart/form-data

Parameters:
- files: List<MultipartFile> (required)
- markFirstAsPrimary: boolean (optional, default: true)
```

#### Get Product Resources
```http
GET /api/products/{productId}/resources
```

#### Delete Product Resource
```http
DELETE /api/products/{productId}/resources/{resourceId}
```

#### Set Primary Resource
```http
PUT /api/products/{productId}/resources/{resourceId}/primary
```

### 3. Static File Access
```http
GET /files/{filePath}
```

## File Validation

### Allowed File Types
- **Images**: jpg, jpeg, png, gif, webp
- **Documents**: pdf, doc, docx, xls, xlsx

### File Size Limits
- **Images**: 5MB
- **Documents**: 10MB
- **General**: 10MB

### Security Features
- File type validation
- File size validation
- Malicious filename detection
- Maximum 10 files per upload

## Sử dụng

### 1. Upload file đơn giản
```bash
curl -X POST "http://localhost:8080/api/files/upload" \
  -F "file=@image.jpg" \
  -F "subDirectory=products" \
  -F "isPrimary=true"
```

### 2. Upload nhiều file cho sản phẩm
```bash
curl -X POST "http://localhost:8080/api/products/{productId}/resources" \
  -F "files=@image1.jpg" \
  -F "files=@image2.jpg" \
  -F "markFirstAsPrimary=true"
```

### 3. Truy cập file
```bash
# Local storage
http://localhost:8080/files/products/uuid-filename.jpg

# S3 storage (sẽ trả về presigned URL)
http://localhost:8080/files/products/uuid-filename.jpg
```

## Response Format

### Success Response
```json
{
  "success": true,
  "message": "File uploaded successfully",
  "files": [
    {
      "originalName": "image.jpg",
      "fileName": "uuid-filename.jpg",
      "fileUrl": "http://localhost:8080/files/products/uuid-filename.jpg",
      "fileType": "image/jpeg",
      "fileSize": 1024000,
      "isPrimary": true
    }
  ]
}
```

### Error Response
```json
{
  "success": false,
  "message": "File type not allowed. Allowed types: jpg, jpeg, png, gif, webp, pdf, doc, docx, xls, xlsx"
}
```

## Cấu trúc thư mục

### Local Storage
```
uploads/
├── products/
│   ├── {productId}/
│   │   ├── image1.jpg
│   │   └── image2.jpg
├── users/
│   └── avatar.jpg
└── documents/
    └── contract.pdf
```

### S3 Storage
```
bucket-name/
├── products/
│   ├── {productId}/
│   │   ├── image1.jpg
│   │   └── image2.jpg
├── users/
│   └── avatar.jpg
└── documents/
    └── contract.pdf
```

## Environment Variables

### Local Storage
```bash
FILE_STORAGE_TYPE=local
```

### AWS S3
```bash
FILE_STORAGE_TYPE=s3
AWS_S3_BUCKET_NAME=your-bucket-name
AWS_S3_REGION=us-east-1
AWS_S3_ACCESS_KEY=your-access-key
AWS_S3_SECRET_KEY=your-secret-key
```

## Lưu ý

1. **Security**: Luôn validate file trước khi upload
2. **Performance**: Sử dụng S3 cho production để tối ưu hiệu suất
3. **Backup**: Cấu hình backup cho local storage
4. **CDN**: Sử dụng CloudFront với S3 để tăng tốc độ truy cập
5. **Monitoring**: Theo dõi dung lượng và chi phí storage

## Troubleshooting

### Lỗi thường gặp

1. **File too large**: Tăng `spring.servlet.multipart.max-file-size`
2. **Invalid file type**: Kiểm tra `FileValidationService`
3. **S3 access denied**: Kiểm tra AWS credentials và permissions
4. **Local storage permission**: Kiểm tra quyền ghi thư mục uploads

### Debug

1. Kiểm tra logs Spring Boot
2. Verify file permissions
3. Test với file nhỏ trước
4. Kiểm tra network connectivity (S3)
