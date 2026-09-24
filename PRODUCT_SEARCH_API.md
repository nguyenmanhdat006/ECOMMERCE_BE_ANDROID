# API Tìm Kiếm Sản Phẩm (Product Search API)

## Mô tả
API tìm kiếm sản phẩm với nhiều bộ lọc và phân trang.

## Endpoint
```
GET /api/products/search
```

## Request Parameters

Tất cả các tham số đều là **optional** (không bắt buộc):

| Tham số | Kiểu dữ liệu | Mô tả | Giá trị mặc định |
|---------|--------------|-------|------------------|
| `keyword` | String | Từ khóa tìm kiếm (tìm trong tên, mô tả, thương hiệu) | - |
| `categoryId` | UUID | ID danh mục sản phẩm | - |
| `typeId` | UUID | ID loại danh mục | - |
| `brand` | String | Thương hiệu (tìm chính xác, không phân biệt hoa thường) | - |
| `minPrice` | BigDecimal | Giá tối thiểu | - |
| `maxPrice` | BigDecimal | Giá tối đa | - |
| `minRating` | Float | Đánh giá tối thiểu (0-5) | - |
| `isNewArrival` | Boolean | Lọc sản phẩm mới về | - |
| `sortBy` | String | Trường để sắp xếp (name, price, rating, createdAt, etc.) | `name` |
| `sortDirection` | String | Hướng sắp xếp: `asc` hoặc `desc` | `asc` |
| `page` | Integer | Số trang (bắt đầu từ 0) | `0` |
| `size` | Integer | Số lượng sản phẩm mỗi trang | `10` |

## Response Format

```json
{
  "products": [
    {
      "id": "uuid",
      "name": "Tên sản phẩm",
      "description": "Mô tả sản phẩm",
      "price": 100000,
      "brand": "Nike",
      "rating": 4.5,
      "isNewArrival": true,
      "slug": "san-pham-1",
      "categoryId": "uuid",
      "categoryName": "Giày",
      "categoryTypeId": "uuid",
      "categoryTypeName": "Thể thao",
      "thumbnail": "url",
      "variants": [],
      "productResources": []
    }
  ],
  "currentPage": 0,
  "totalItems": 100,
  "totalPages": 10,
  "pageSize": 10,
  "hasNext": true,
  "hasPrevious": false
}
```

## Ví dụ sử dụng

### 1. Tìm kiếm theo từ khóa
```http
GET /api/products/search?keyword=giày
```
Tìm tất cả sản phẩm có chứa từ "giày" trong tên, mô tả hoặc thương hiệu.

### 2. Tìm kiếm và lọc theo giá
```http
GET /api/products/search?keyword=áo&minPrice=100000&maxPrice=500000
```
Tìm sản phẩm "áo" có giá từ 100,000 đến 500,000 VNĐ.

### 3. Lọc theo danh mục và thương hiệu
```http
GET /api/products/search?categoryId=123e4567-e89b-12d3-a456-426614174000&brand=Nike
```
Lọc sản phẩm thuộc danh mục cụ thể và thương hiệu Nike.

### 4. Tìm kiếm với sắp xếp
```http
GET /api/products/search?keyword=giày&sortBy=price&sortDirection=desc
```
Tìm "giày" và sắp xếp theo giá giảm dần.

### 5. Lọc sản phẩm mới với đánh giá cao
```http
GET /api/products/search?isNewArrival=true&minRating=4.0&sortBy=rating&sortDirection=desc
```
Lấy sản phẩm mới về có đánh giá >= 4 sao, sắp xếp theo rating giảm dần.

### 6. Phân trang
```http
GET /api/products/search?page=2&size=20
```
Lấy trang thứ 3 (page bắt đầu từ 0) với 20 sản phẩm mỗi trang.

### 7. Tìm kiếm tổng hợp
```http
GET /api/products/search?keyword=giày thể thao&categoryId=123e4567-e89b-12d3-a456-426614174000&minPrice=200000&maxPrice=1000000&minRating=3.5&sortBy=price&sortDirection=asc&page=0&size=15
```
Tìm kiếm với nhiều điều kiện:
- Từ khóa: "giày thể thao"
- Danh mục cụ thể
- Giá từ 200,000 đến 1,000,000 VNĐ
- Đánh giá >= 3.5 sao
- Sắp xếp theo giá tăng dần
- Lấy 15 sản phẩm đầu tiên

## Các trường có thể sắp xếp (sortBy)

- `name` - Tên sản phẩm
- `price` - Giá
- `rating` - Đánh giá
- `brand` - Thương hiệu
- `createdAt` - Ngày tạo
- `updatedAt` - Ngày cập nhật

## Lưu ý

1. **Tìm kiếm không phân biệt hoa thường**: Từ khóa và thương hiệu không phân biệt chữ hoa/thường.

2. **Tìm kiếm mềm với keyword**: Tham số `keyword` sẽ tìm các sản phẩm có chứa từ khóa trong:
   - Tên sản phẩm
   - Mô tả
   - Thương hiệu

3. **Tìm kiếm chính xác với brand**: Tham số `brand` tìm kiếm chính xác tên thương hiệu.

4. **Phân trang**: 
   - `page` bắt đầu từ 0
   - Response trả về thông tin phân trang đầy đủ để xử lý ở frontend

5. **Performance**: API sử dụng JPA Specification nên có hiệu suất tốt với database lớn.

## Status Codes

- `200 OK` - Tìm kiếm thành công
- `400 Bad Request` - Tham số không hợp lệ
- `500 Internal Server Error` - Lỗi server

## Testing với cURL

```bash
# Tìm kiếm đơn giản
curl -X GET "http://localhost:8080/api/products/search?keyword=giày"

# Tìm kiếm với nhiều filter
curl -X GET "http://localhost:8080/api/products/search?keyword=giày&minPrice=100000&maxPrice=500000&sortBy=price&sortDirection=asc&page=0&size=10"

# Lọc theo danh mục
curl -X GET "http://localhost:8080/api/products/search?categoryId=123e4567-e89b-12d3-a456-426614174000"
```

## Testing với JavaScript/Axios

```javascript
// Tìm kiếm sản phẩm
const searchProducts = async (searchParams) => {
  try {
    const response = await axios.get('/api/products/search', {
      params: {
        keyword: searchParams.keyword,
        categoryId: searchParams.categoryId,
        minPrice: searchParams.minPrice,
        maxPrice: searchParams.maxPrice,
        minRating: searchParams.minRating,
        sortBy: searchParams.sortBy || 'name',
        sortDirection: searchParams.sortDirection || 'asc',
        page: searchParams.page || 0,
        size: searchParams.size || 10
      }
    });
    
    return response.data;
  } catch (error) {
    console.error('Search error:', error);
    throw error;
  }
};

// Ví dụ sử dụng
const results = await searchProducts({
  keyword: 'giày',
  minPrice: 100000,
  maxPrice: 500000,
  sortBy: 'price',
  sortDirection: 'asc',
  page: 0,
  size: 20
});

console.log('Sản phẩm:', results.products);
console.log('Tổng số:', results.totalItems);
console.log('Tổng trang:', results.totalPages);
```

