# API Contract – ShopEase Backend

Phiên bản 1.0 – 13/10/2026. Nội dung được lập từ mã nguồn và **đã kiểm chứng bằng cách gọi thật từng API** (bộ test `ApiFlowIT`, xem mục 14). Các ví dụ phản hồi là kết quả thật; riêng id, email, token đã rút gọn.

## 0. Quy ước chung

| Hạng mục | Quy ước |
| --- | --- |
| Base URL | `http://localhost:8080` (cổng 8080) |
| Định dạng | JSON, UTF-8, header `Content-Type: application/json` (tải ảnh dùng `multipart/form-data`) |
| Xác thực | Header `Authorization: Bearer <token>`; token lấy từ `POST /api/auth/login`, sống 3600 giây |
| Kiểu id | UUID (chuỗi 36 ký tự) |
| Tiền | Số thập phân, đơn vị VND (ví dụ `199000`, `199000.00`) |
| Thời gian (dữ liệu) | ISO-8601 UTC, ví dụ `2026-10-12T21:11:35.107+00:00` |
| Thời gian (`ErrorResponse.timestamp`) | ISO-8601 giờ máy chủ, ví dụ `2026-10-13T04:11:09.939171` |
| Trường lạ trong request | Bị bỏ qua, không báo lỗi |
| Cột "Quyền" | **Public** không cần token; **User** cần đăng nhập (USER hoặc ADMIN); **Admin** cần vai trò ADMIN |

### 0.1. Định dạng lỗi chuẩn (`ErrorResponse`)

```json
{
  "timestamp": "2026-10-13T04:11:41.6059634",
  "status": 400,
  "error": "Bad Request",
  "message": "Không thể chuyển từ trạng thái PENDING sang PAID",
  "path": "/api/orders/f69d7dbd-.../status"
}
```

Ngoại lệ: các API `register` trả lỗi dạng `{ "code": 400, "message": "..." }`; lỗi 403 của địa chỉ có thân rỗng.

### 0.2. Mã HTTP chung

| Mã | Khi nào |
| --- | --- |
| 200 | Thành công (đọc, sửa, một số lệnh xóa) |
| 201 | Tạo mới thành công |
| 204 | Xóa thành công, không có nội dung (chỉ sản phẩm và người dùng); các lệnh xóa khác trả 200 thân rỗng |
| 302 | Chuyển hướng (callback VNPAY) |
| 400 | Dữ liệu sai, sai kiểu (ví dụ UUID sai định dạng), chuyển trạng thái đơn sai luồng |
| 401 | Chưa đăng nhập, token sai hoặc hết hạn, sai email hoặc mật khẩu |
| 403 | Đã đăng nhập nhưng không đủ quyền |
| 404 | Không tìm thấy dữ liệu |
| 500 | Lỗi máy chủ (có một số lỗi nghiệp vụ đang trả 500, xem mục 14.2) |

Ví dụ 401 và 403:

```json
{ "timestamp": "2026-10-13T04:14:15.222", "status": 401, "error": "Unauthorized", "message": "You are not authorized to access this resource", "path": "/api/orders/me" }
{ "timestamp": "2026-10-13T04:14:22.846", "status": 403, "error": "Forbidden", "message": "You do not have permission to access this resource", "path": "/api/products" }
```

## 1. Mục lục API

| Nhóm | Endpoint | Quyền |
| --- | --- | --- |
| Xác thực | `POST /api/auth/register`, `POST /api/auth/verify`, `POST /api/auth/login`, `GET /api/user/profile` | Public, trừ profile là User |
| Quản trị người dùng | `GET/POST /api/admin/users`, `GET/PUT/DELETE /api/admin/users/{id}` | Admin |
| Danh mục | `GET /api/category[/{id}]`; `POST/PUT/DELETE` | Public (GET) / Admin |
| Loại danh mục | `GET /api/category-types[/{id}]`; `POST/PUT/DELETE` | Public (GET) / Admin |
| Sản phẩm | `GET /api/products`, `GET /api/products/search`, `GET /api/products/{id}`; `POST/PUT/DELETE` | Public (GET) / Admin |
| Ảnh sản phẩm | `GET/POST /api/products/{id}/resources`, `PUT .../{rid}/primary`, `DELETE .../{rid}` | Public (GET) / Admin |
| Biến thể | `GET /api/product-variants[/{id}]`; `POST/PUT/DELETE` | User (GET) / Admin |
| Địa chỉ | `/api/addresses...` | User |
| Giỏ hàng | `/api/carts...` | User |
| Đơn hàng | `/api/orders...` | User / Admin |
| Thanh toán VNPAY | `POST /api/vnpay/create-payment`, `GET /api/vnpay/return` | User / Public |
| Đánh giá | `/api/reviews...` | Public (GET) / User |
| Phiếu nhập kho | `/api/inventory-receipts...` | Admin |
| Dashboard | `GET /api/dashboard/kpi`, `POST /api/dashboard/refresh`, WebSocket `/ws/dashboard` | Admin / Public (WS) |
| Hệ thống | `GET /api/health`, `GET /uploads/**`, `/swagger-ui.html`, `/v3/api-docs` | Public |

## 2. Xác thực và hồ sơ

### 2.1. `POST /api/auth/register` – Đăng ký (Public)

Tạo tài khoản USER chưa kích hoạt và gửi mã xác thực 6 số qua email.

| Trường | Kiểu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `email` | string | Có | Duy nhất; phải chứa `@` |
| `password` | string | Có | Tối thiểu 6 ký tự |
| `firstName` | string | Không | Tên |
| `lastName` | string | Không | Họ |
| `phoneNumber` | string | Không | Số điện thoại |

Request:

```json
{ "firstName": "Lan", "lastName": "Nguyen", "email": "lan@example.com", "password": "Test@12345", "phoneNumber": "0911222333" }
```

| Mã | Phản hồi |
| --- | --- |
| 200 | `{ "code": 200, "message": "User created!" }` |
| 400 | `{ "code": 400, "message": "Email already exist!" }`, hoặc `"Email is invalid!"`, hoặc `"Password must be at least 6 characters!"` |

Gửi email thất bại không làm lỗi đăng ký; khi chưa cấu hình `MAIL_PASSWORD`, lấy mã từ cột `verification_code` trong DB.

### 2.2. `POST /api/auth/verify` – Kích hoạt tài khoản (Public)

| Trường | Kiểu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `userName` | string | Có | Email đã đăng ký |
| `code` | string | Có | Mã 6 số |

```json
{ "userName": "lan@example.com", "code": "483920" }
```

| Mã | Phản hồi |
| --- | --- |
| 200 | Thân rỗng; tài khoản được kích hoạt |
| 400 | Thân rỗng; sai mã hoặc không có tài khoản |

### 2.3. `POST /api/auth/login` – Đăng nhập (Public)

| Trường | Kiểu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `userName` | string | Có | Email |
| `password` | string | Có | Mật khẩu |

Phản hồi 200:

```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "user": {
    "id": "bdf369c9-9f72-49c5-8b15-5826d6ac2c04",
    "firstName": "Lan", "lastName": "Nguyen", "avatar": null,
    "email": "lan@example.com", "phoneNumber": "0911222333",
    "enabled": true, "role": "USER"
  }
}
```

Token là JWT, claim: `iss=authapp`, `sub=<email>`, `role=USER|ADMIN`, `iat`, `exp` (sau 3600 giây).

| Mã | Khi nào |
| --- | --- |
| 401 | Sai mật khẩu, email không tồn tại, hoặc tài khoản chưa kích hoạt: `message = "Invalid username or password"` |

### 2.4. `GET /api/user/profile` – Hồ sơ của tôi (User)

Phản hồi 200:

```json
{
  "id": "bdf369c9-...", "firstName": "Lan", "lastName": "Nguyen",
  "phoneNumber": "0911222333", "email": "lan@example.com", "role": "USER",
  "authorityList": [ { "id": "92f3ef01-...", "roleCode": "USER", "roleDescription": "Customer", "authority": "USER" } ],
  "avatar": null,
  "addressList": []
}
```

Lỗi: 401 nếu chưa đăng nhập.

## 3. Quản trị người dùng (Admin)

Đối tượng `UserDto`: `id`, `firstName`, `lastName`, `avatar`, `email`, `phoneNumber`, `enabled`, `role`.

| Method | Endpoint | Mô tả | Mã thành công |
| --- | --- | --- | --- |
| GET | `/api/admin/users` | Danh sách người dùng (mảng `UserDto`) | 200 |
| GET | `/api/admin/users/{id}` | Chi tiết; 404 nếu không có (`"User not found"`) | 200 |
| POST | `/api/admin/users` | Tạo người dùng | 201 |
| PUT | `/api/admin/users/{id}` | Sửa một phần, chỉ cập nhật trường được gửi | 200 |
| DELETE | `/api/admin/users/{id}` | Vô hiệu hóa (đặt `enabled=false`), không xóa dòng | 204 |

Request tạo hoặc sửa (mọi trường tùy chọn khi sửa; khi tạo cần ít nhất `email`):

| Trường | Kiểu | Mô tả |
| --- | --- | --- |
| `email` | string | Email (duy nhất) |
| `firstName`, `lastName`, `phoneNumber`, `avatar` | string | Thông tin cá nhân |
| `enabled` | boolean | Khi tạo, mặc định `false` nếu không gửi |
| `role` | string | `USER` hoặc `ADMIN`; sai giá trị trả **400** `"Invalid role: ..."`; khi tạo mặc định `USER` |

```json
{ "firstName": "Tao", "lastName": "Moi", "email": "new@example.com", "phoneNumber": "0900111222", "enabled": true, "role": "USER" }
```

Người dùng tạo từ trang quản trị chưa có mật khẩu nên chưa đăng nhập được.

## 4. Danh mục và loại danh mục

### 4.1. Danh mục (`/api/category`)

| Method | Endpoint | Quyền | Mã |
| --- | --- | --- | --- |
| GET | `/api/category` | Public | 200 |
| GET | `/api/category/{id}` | Public | 200 |
| POST | `/api/category` | Admin | 201 |
| PUT | `/api/category/{id}` | Admin | 200 |
| DELETE | `/api/category/{id}` | Admin | 200 (thân rỗng) |

Request tạo/sửa:

| Trường | Kiểu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `name` | string | Có | Tên hiển thị |
| `code` | string | Có | Mã danh mục |
| `description` | string | Có | Mô tả |

Phản hồi (GET danh sách rút gọn):

```json
[
  { "id": "48ac3693-...", "name": "Thời trang nam", "code": "MEN", "description": "Quần áo dành cho nam",
    "categoryTypes": [ { "id": "c966b496-...", "name": "Áo thun", "code": "MEN_TSHIRT", "description": "Áo thun" } ] }
]
```

Phản hồi POST 201: `{ "id": "...", "name": "...", "code": "...", "description": "...", "categoryTypes": null }`.

### 4.2. Loại danh mục (`/api/category-types`)

| Method | Endpoint | Quyền | Mã |
| --- | --- | --- | --- |
| GET | `/api/category-types`, `/api/category-types/{id}` | Public | 200 |
| POST | `/api/category-types` | Admin | 201 |
| PUT | `/api/category-types/{id}` | Admin | 200 |
| DELETE | `/api/category-types/{id}` | Admin | 200 (thân rỗng) |

Request tạo/sửa:

| Trường | Kiểu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `name` | string | Có | Tên loại |
| `code` | string | Có | Mã loại |
| `description` | string | Không | Mô tả |
| `categoryId` | uuid | Có | Danh mục cha |

Phản hồi: `{ "id": "0af7e4e0-...", "name": "...", "code": "...", "description": "..." }`.

Xóa loại còn sản phẩm tham chiếu trả **500** (lỗi khóa ngoại), chưa phải 409.

## 5. Sản phẩm

### 5.1. Đối tượng `ProductDto` (dùng cho GET)

```json
{
  "id": "155acfe9-...", "name": "Áo polo pique", "description": "Áo polo vải cá sấu, cổ dệt bo.",
  "price": 349000.00, "brand": "Routine", "rating": 0.0,
  "categoryId": "48ac3693-...", "categoryName": "Thời trang nam",
  "categoryTypeId": "c966b496-...", "categoryTypeName": "Áo thun",
  "thumbnail": "https://placehold.co/600x800/png?text=ao+polo+pique",
  "slug": "ao-polo-pique", "newArrival": false,
  "variants": [ { "id": "616a9b3d-...", "color": "Xanh navy", "size": "M", "stockQuantity": 20 } ],
  "productResources": [ { "id": "79ed1536-...", "name": "ao-polo-pique-1.png", "url": "https://...", "type": "image", "isPrimary": true } ]
}
```

Lưu ý: cờ hàng mới có tên JSON là **`newArrival`** (không phải `isNewArrival`).

### 5.2. Xem sản phẩm (Public)

| Endpoint | Mô tả | Phản hồi |
| --- | --- | --- |
| `GET /api/products` | Danh sách; tham số tùy chọn `categoryId`, `typeId`, `slug` | 200, mảng `ProductDto` |
| `GET /api/products/{id}` | Chi tiết | 200 `ProductDto`; 404 `"Product Not Found!"`; 400 nếu id không phải UUID |
| `GET /api/products/search` | Tìm kiếm có phân trang | 200, xem bên dưới |

Tham số của `GET /api/products/search` (đều tùy chọn):

| Tham số | Kiểu | Mặc định | Mô tả |
| --- | --- | --- | --- |
| `keyword` | string | – | Tìm trong tên, mô tả, thương hiệu; **phân biệt dấu** (`ao` không khớp `Áo`) |
| `categoryId`, `typeId` | uuid | – | Lọc danh mục, loại |
| `brand` | string | – | Khớp chính xác, không phân biệt hoa thường |
| `minPrice`, `maxPrice` | số | – | Khoảng giá |
| `minRating` | số | – | Điểm tối thiểu |
| `isNewArrival` | boolean | – | Chỉ hàng mới |
| `sortBy` | string | `name` | Trường sắp xếp |
| `sortDirection` | `asc` / `desc` | `asc` | Chiều sắp xếp |
| `page` | số | `0` | Trang, bắt đầu từ 0 |
| `size` | số | `10` | Kích thước trang |

Phản hồi 200:

```json
{ "totalItems": 1, "totalPages": 1, "pageSize": 10, "currentPage": 0, "hasNext": false, "hasPrevious": false, "products": [ { "...": "ProductDto" } ] }
```

### 5.3. Thêm, sửa, xóa sản phẩm (Admin)

| Method | Endpoint | Mã |
| --- | --- | --- |
| POST | `/api/products` | 201 |
| PUT | `/api/products/{id}` | 200 |
| DELETE | `/api/products/{id}` | 204 |

Request tạo (`ProductDto`):

| Trường | Kiểu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `name` | string | Có | Tên |
| `price` | số | Có | Giá niêm yết, không âm |
| `brand` | string | Có | Thương hiệu |
| `slug` | string | Có | Duy nhất |
| `categoryId` | uuid | Có | Danh mục |
| `categoryTypeId` | uuid | Có | Loại |
| `description` | string | Không | Tối đa 255 ký tự |
| `newArrival` | boolean | Không | Hàng mới |
| `variants` | mảng | Không | Mỗi phần tử: `color`, `size`, `stockQuantity` (số nguyên ≥ 0) |
| `productResources` | mảng | Không | Mỗi phần tử: `name`, `url`, `type`, `isPrimary` |

```json
{
  "name": "Áo thun cotton", "description": "Áo thun 100% cotton", "price": 199000, "brand": "Coolmate",
  "newArrival": true, "slug": "ao-thun-cotton", "categoryId": "48ac3693-...", "categoryTypeId": "c966b496-...",
  "variants": [ { "color": "Đen", "size": "M", "stockQuantity": 10 }, { "color": "Đen", "size": "L", "stockQuantity": 5 } ]
}
```

Phản hồi 201 và 200 là **entity sản phẩm** (không phải `ProductDto`): có `createdAt`, `updatedAt`, `productVariants` (kèm các mảng `orderItems`, `cartItems`... rỗng hoặc null), `resources`, `newArrival`. Nên gọi lại `GET /api/products/{id}` để lấy dạng chuẩn.

PUT ghi đè toàn bộ thông tin từ nội dung gửi lên, vì vậy cần gửi đầy đủ các trường bắt buộc; trường không gửi sẽ thành `null` (ví dụ `rating`). Xóa sản phẩm là xóa cứng và kéo theo biến thể, ảnh, dòng giỏ hàng.

### 5.4. Ảnh sản phẩm

| Method | Endpoint | Quyền | Mã |
| --- | --- | --- | --- |
| GET | `/api/products/{productId}/resources` | Public | 200 |
| POST | `/api/products/{productId}/resources` | Admin | 200 |
| PUT | `/api/products/{productId}/resources/{resourceId}/primary` | Admin | 200 |
| DELETE | `/api/products/{productId}/resources/{resourceId}` | Admin | 200 |

`POST` dùng `multipart/form-data`:

| Trường form | Kiểu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `files` | file (nhiều) | Có | Tối đa 10 MB mỗi file và mỗi yêu cầu |
| `markFirstAsPrimary` | boolean | Không | Mặc định `true`: ảnh đầu tiên là ảnh chính |

Phản hồi POST 200:

```json
{
  "message": "Product resources uploaded successfully", "success": true,
  "files": [ { "originalName": "a.png", "fileName": "products/<productId>/<uuid>.png",
               "fileUrl": "http://localhost:8080/uploads/products/<productId>/<uuid>.png",
               "fileType": "image/png", "fileSize": 70, "primary": true } ]
}
```

GET 200: `[ { "id": "...", "name": "a.png", "url": "http://localhost:8080/uploads/...", "type": "image/png", "isPrimary": true } ]`.
PUT và DELETE 200: `{ "message": "Primary resource updated successfully", "success": true, "files": null }` (DELETE: `"Product resource deleted successfully"`).

### 5.5. Biến thể (`/api/product-variants`)

| Method | Endpoint | Quyền | Ghi chú |
| --- | --- | --- | --- |
| GET | `/api/product-variants`, `/api/product-variants/{id}` | User | 200; 401 nếu chưa đăng nhập |
| POST, PUT, DELETE | `/api/product-variants[/{id}]` | Admin | **Không dùng được**: xem 14.2 |

GET trả entity: `{ "id", "color", "size", "stockQuantity", "orderItems": [], "cartItems": [], "inventoryReceiptItems": [], "stockMovements": [] }`.

Hiện biến thể chỉ tạo được kèm sản phẩm (mục 5.3). `POST /api/product-variants` luôn lỗi 500 vì trường `product` không đọc được từ JSON.

## 6. Địa chỉ (`/api/addresses`, User)

Đối tượng địa chỉ: `id`, `name`, `street`, `city`, `state`, `zipCode`, `phoneNumber` (phản hồi danh sách có thêm `userId`).

| Method | Endpoint | Mô tả | Mã |
| --- | --- | --- | --- |
| GET | `/api/addresses/my-addresses` | Địa chỉ của tôi | 200 |
| POST | `/api/addresses` | Thêm địa chỉ cho người đang đăng nhập | 201 |
| PUT | `/api/addresses/{id}` | Sửa; chỉ chủ sở hữu, người khác nhận **403** thân rỗng | 200 |
| DELETE | `/api/addresses/{id}` | Xóa; chỉ chủ sở hữu, người khác **403** | 200 (rỗng) |
| GET | `/api/addresses/user/{userId}` | Địa chỉ của một user (404 nếu user không tồn tại) | 200 |
| GET | `/api/addresses`, `/api/addresses/{id}` | Tra cứu | 200 |

Request thêm/sửa:

| Trường | Kiểu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `street` | string | Có | Số nhà, đường |
| `city` | string | Có | Thành phố |
| `state` | string | Có | Tỉnh, quận |
| `zipCode` | string | Có | Mã bưu chính |
| `phoneNumber` | string | Có | Số điện thoại nhận hàng |
| `name` | string | Không | Tên gợi nhớ |

```json
{ "name": "Nhà riêng", "street": "12 Nguyễn Trãi", "city": "Hồ Chí Minh", "state": "Quận 1", "zipCode": "700000", "phoneNumber": "0911222333" }
```

Phản hồi 201: `{ "id": "3b03417e-...", "name": "Nhà riêng", "street": "...", "city": "...", "state": "...", "zipCode": "...", "phoneNumber": "..." }`.

## 7. Giỏ hàng (`/api/carts`, User)

Đối tượng `CartDto`: `id`, `quantity`, `createdAt`, `updatedAt`, `userId`, `productId`, `productVariantId`.

| Method | Endpoint | Mô tả | Mã |
| --- | --- | --- | --- |
| POST | `/api/carts` | Thêm dòng giỏ | 201 |
| PUT | `/api/carts/{id}` | Sửa dòng giỏ (gửi lại đủ các trường) | 200 |
| GET | `/api/carts/{id}` | Một dòng giỏ (`CartDto`) | 200 |
| GET | `/api/carts/user/{userId}` | Giỏ của một user kèm thông tin sản phẩm | 200 |
| GET | `/api/carts` | Tất cả dòng giỏ | 200 |
| DELETE | `/api/carts/{id}` | Xóa dòng giỏ | 200 (rỗng) |

Request thêm/sửa:

| Trường | Kiểu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `userId` | uuid | Có | Chủ giỏ (hiện do client gửi) |
| `productId` | uuid | Có | Sản phẩm |
| `productVariantId` | uuid | Có | Biến thể |
| `quantity` | số nguyên | Có | Lớn hơn 0 |

Phản hồi `GET /api/carts/user/{userId}`:

```json
[ {
  "id": "a6d48cac-...",
  "product": { "id": "7c710c58-...", "name": "Ao test", "price": 249000.00, "thumbnail": "http://localhost:8080/uploads/...", "newArrival": false },
  "productVariant": { "id": "f26cc957-...", "color": "Den", "size": "M", "stockQuantity": 10 },
  "quantity": 2, "createdAt": "2026-10-12T21:11:32.321+00:00", "updatedAt": "2026-10-12T21:11:32.321+00:00",
  "userId": "bdf369c9-..."
} ]
```

Hệ thống chưa kiểm tra `quantity` so với tồn kho (thêm 9999 vẫn trả 201).

## 8. Đơn hàng (`/api/orders`)

### 8.1. Bảng endpoint

| Method | Endpoint | Quyền | Mô tả | Mã |
| --- | --- | --- | --- | --- |
| POST | `/api/orders` | User | Tạo đơn | 201 |
| GET | `/api/orders/me` | User | Đơn của tôi (theo token) | 200 |
| GET | `/api/orders/unreviewed` | User | Chi tiết đơn đã mua chưa đánh giá | 200 |
| GET | `/api/orders/{id}` | User | Chi tiết đơn (404 nếu không có) | 200 |
| GET | `/api/orders` | **Admin** | Tất cả đơn | 200 |
| PATCH | `/api/orders/{id}/status?changedBy=` | **Admin** | Đổi trạng thái đơn | 200 |
| PATCH | `/api/orders/{id}/payment-status?changedBy=` | User | Đổi trạng thái thanh toán | 200 |
| DELETE | `/api/orders/{id}` | User | Xóa đơn | 200 (rỗng) |

### 8.2. `POST /api/orders` – Tạo đơn

| Trường | Kiểu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `customerId` | uuid | Có | Người đặt (hiện do client gửi) |
| `orderNumber` | string | Có | Mã đơn, **duy nhất** |
| `totalAmount` | số | Có | Tổng tiền (VND) |
| `status` | string | Có | Trạng thái ban đầu, dùng `PENDING` |
| `paymentMethod` | string | Có | `COD` hoặc `VNPAY` |
| `shippingAddress` | string | Có | Địa chỉ giao dạng chuỗi |
| `notes` | string | Không | Ghi chú |
| `orderItems` | mảng | Có | Danh sách dòng chi tiết |
| `orderItems[].productId` | uuid | Có | Sản phẩm |
| `orderItems[].productVariantId` | uuid | Có | Biến thể |
| `orderItems[].quantity` | số nguyên | Có | Lớn hơn 0 |
| `orderItems[].unitPrice` | số | Có | Đơn giá |
| `orderItems[].totalPrice` | số | Có | Thành tiền |

```json
{
  "customerId": "bdf369c9-...", "orderNumber": "SE-20261013-001", "totalAmount": 498000, "status": "PENDING",
  "paymentMethod": "COD", "shippingAddress": "Nguyen Van A - 0911222333 - 12 Nguyễn Trãi, Quận 1, HCM", "notes": "Giao giờ hành chính",
  "orderItems": [ { "productId": "7c710c58-...", "productVariantId": "f26cc957-...", "quantity": 2, "unitPrice": 249000, "totalPrice": 498000 } ]
}
```

Phản hồi 201 (entity đơn hàng):

```json
{
  "id": "f69d7dbd-...", "orderNumber": "SE-20261013-001", "orderDate": "2026-10-12T21:11:35.107+00:00",
  "totalAmount": 498000, "status": "PENDING", "paymentStatus": "UNPAID", "paymentMethod": "COD",
  "shippingAddress": "...", "notes": "Giao giờ hành chính",
  "orderItems": [ { "id": "b5b8ca9a-...", "quantity": 2, "unitPrice": 249000, "totalPrice": 498000, "isReviewed": false } ],
  "updatedAt": null
}
```

Lưu ý: máy chủ lưu nguyên số liệu client gửi (không tính lại giá), **chưa trừ tồn kho**, chưa xóa giỏ hàng. Trùng `orderNumber` trả 500.

### 8.3. `GET /api/orders/me`, `GET /api/orders/{id}`, `GET /api/orders` – Dạng `OrderResponse`

```json
{
  "id": "f69d7dbd-...", "orderNumber": "SE-20261013-001", "orderDate": "2026-10-12T21:11:35.107+00:00",
  "totalAmount": 498000.00, "status": "PENDING", "paymentMethod": "COD",
  "shippingAddress": "...", "notes": "...", "customerId": "bdf369c9-...",
  "orderItems": [ {
    "id": "b5b8ca9a-...", "quantity": 2, "unitPrice": 249000.00, "totalPrice": 498000.00,
    "productId": "7c710c58-...", "productVariantId": "f26cc957-...", "productName": "Ao test",
    "isReviewed": false, "thumbnail": "http://localhost:8080/uploads/..."
  } ]
}
```

`OrderResponse` **không có `paymentStatus`**; chỉ phản hồi của POST và PATCH (entity) mới có. `GET /api/orders/unreviewed` trả mảng `OrderItemResponse` (cùng cấu trúc phần tử `orderItems` ở trên).

### 8.4. `PATCH /api/orders/{id}/status?changedBy=<tên>` – Đổi trạng thái (Admin)

Tham số query `changedBy` (bắt buộc): tên người thực hiện, ghi vào lịch sử. Body:

```json
{ "status": "SHIPPING" }
```

Giá trị `status`: `PENDING`, `SHIPPING`, `WAIT_DELIVER`, `PAID`, `CANCELED`, `REFUND`. Luồng hợp lệ:

| Từ | Sang |
| --- | --- |
| `PENDING` | `SHIPPING`, `CANCELED` |
| `SHIPPING` | `WAIT_DELIVER`, `CANCELED` |
| `WAIT_DELIVER` | `PAID`, `REFUND` |
| `PAID` | `REFUND` |
| `CANCELED`, `REFUND` | (không chuyển tiếp) |

Phản hồi 200: entity đơn như mục 8.2 (với `status` mới, `updatedAt` có giá trị). Lỗi 400:

```json
{ "status": 400, "error": "Bad Request", "message": "Không thể chuyển từ trạng thái PENDING sang PAID", "path": "..." }
```

Giá trị `status` sai tên trả 400 `"No enum constant com.nguyendat.shopee_be.entities.OrderStatus.ABC"`. Chuyển sang `CANCELED` hoặc `REFUND` cộng trả số lượng vào kho từng biến thể. Mỗi lần đổi ghi một dòng `order_status_history`.

### 8.5. `PATCH /api/orders/{id}/payment-status?changedBy=` – Trạng thái thanh toán

Body `{ "status": "PAID" }` với `status` thuộc `UNPAID`, `PAID`, `FAILED`, `REFUND`. Phản hồi 200: entity đơn (có `paymentStatus`). Hiện mọi người dùng đã đăng nhập đều gọi được (xem 14.2).

### 8.6. `DELETE /api/orders/{id}`

200 thân rỗng nếu đơn chưa có lịch sử trạng thái. Đơn đã có lịch sử trạng thái trả **500** do khóa ngoại. Mọi người dùng đăng nhập xóa được đơn bất kỳ (xem 14.2).

## 9. Thanh toán VNPAY

### 9.1. `POST /api/vnpay/create-payment` (User)

| Trường | Kiểu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `orderId` | uuid | Có | Mã đơn (404 nếu không tồn tại) |
| `amount` | số | Có | Số tiền VND; gửi VNPAY nhân 100 |

```json
{ "orderId": "ccf79480-...", "amount": 249000 }
```

Phản hồi 200 (text thuần, một URL đã ký HMAC-SHA512):

```text
https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=24900000&vnp_Command=pay&vnp_CreateDate=20261013041151&vnp_CurrCode=VND&...&vnp_SecureHash=...
```

Cần cấu hình `VNPAY_TMN_CODE` và `VNPAY_HASH_SECRET`; `vnp_ReturnUrl` trỏ về frontend (`VNPAY_RETURN_URL_BASE/vnpay-done/{orderId}`).

### 9.2. `GET /api/vnpay/return` (Public)

Frontend gọi lại endpoint này với toàn bộ tham số VNPAY trả về. Tham số cần có: `vnp_TxnRef` (mã đơn), `vnp_ResponseCode`, `vnp_Amount`, `vnp_BankCode`, `vnp_CardType`, `vnp_OrderInfo`, `vnp_PayDate`, `vnp_TransactionNo`.

Kết quả 302, header `Location`:

```text
http://localhost:5173/vnpay-done?orderId=<id>&vnp_Amount=...&vnp_BankCode=...&vnp_CardType=...&vnp_OrderInfo=...&vnp_PayDate=...&vnp_TransactionNo=...&status=success
```

`vnp_ResponseCode=00` thì đơn thành `payment_status = PAID` và `status=success`; khác thì `FAILED` và `status=fail`. Endpoint **không kiểm tra chữ ký** `vnp_SecureHash` và không đối chiếu số tiền; không có `vnp_*` bắt buộc sẽ lỗi (suy ra từ mã nguồn).

## 10. Đánh giá (`/api/reviews`)

| Method | Endpoint | Quyền | Mô tả | Mã |
| --- | --- | --- | --- | --- |
| POST | `/api/reviews` | User | Viết đánh giá | 200 |
| GET | `/api/reviews/product/{productId}` | Public | Đánh giá của sản phẩm | 200 |
| PUT | `/api/reviews/{id}` | User | Sửa đánh giá | 200 |
| DELETE | `/api/reviews/{id}?userId=<uuid>` | User | Xóa đánh giá của mình | 200 |

Request tạo/sửa:

| Trường | Kiểu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `userId` | uuid | Có | Người viết (hiện do client gửi) |
| `productId` | uuid | Có | Sản phẩm |
| `orderItemId` | uuid | Có | Chi tiết đơn đã mua, phải thuộc đơn của `userId` |
| `rating` | số nguyên | Có | 1 đến 5 |
| `comment` | string | Không | Nhận xét |

```json
{ "userId": "bdf369c9-...", "productId": "7c710c58-...", "orderItemId": "b5b8ca9a-...", "rating": 5, "comment": "Vải mát, giao nhanh" }
```

Phản hồi POST/PUT 200: `{ "id": "9d300bf9-...", "rating": 5, "comment": "...", "createdAt": "...", "updatedAt": "..." }`. Sau khi tạo, `order_item.is_reviewed = true` và `product.rating` được tính lại (đã kiểm chứng: đánh giá 5 sao thì rating sản phẩm = 5.0).

Phản hồi GET:

```json
[ { "id": "9d300bf9-...", "rating": 5, "comment": "...", "createdAt": "...", "updatedAt": "...",
    "user": { "id": "bdf369c9-...", "firstName": "Lan", "lastName": "Nguyen", "avatar": null,
              "email": "lan@example.com", "phoneNumber": "0911222333", "enabled": true, "role": null } } ]
```

Lưu ý: API công khai này **trả cả email và số điện thoại** người đánh giá (xem 14.2).

Lỗi: viết cho item không thuộc mình trả **500** với `message = "Bạn không có quyền đánh giá sản phẩm này!"`; `rating` ngoài 1 đến 5 trả **500** (vi phạm ràng buộc DB).

## 11. Phiếu nhập kho (`/api/inventory-receipts`, Admin)

| Method | Endpoint | Mô tả | Mã |
| --- | --- | --- | --- |
| GET | `/api/inventory-receipts` | Danh sách (kèm dòng chi tiết) | 200 |
| GET | `/api/inventory-receipts/{id}` | Chi tiết | 200 |
| POST | `/api/inventory-receipts` | Tạo phiếu | 201 |
| PUT | `/api/inventory-receipts/{id}` | Sửa phiếu | 200 |
| DELETE | `/api/inventory-receipts/{id}` | Xóa phiếu | 200 (rỗng) |

Request tạo:

| Trường | Kiểu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `receiptNumber` | string | Có | Số phiếu, duy nhất |
| `supplierName` | string | Có | Nhà cung cấp |
| `totalAmount` | số | Có | Tổng giá trị |
| `status` | string | Có | `PENDING`, `APPROVED`, `CANCELLED` (chuỗi tự do) |
| `notes` | string | Không | Ghi chú |

```json
{ "receiptNumber": "PN-001", "supplierName": "Công ty May Việt Tiến", "totalAmount": 1000000, "status": "PENDING", "notes": "Phiếu test" }
```

Phản hồi 201: `{ "id": "...", "receiptNumber": "PN-001", "supplierName": "...", "receiptDate": "2026-10-12T21:11:55.676+00:00", "totalAmount": 1000000, "status": "PENDING", "notes": "...", "inventoryReceiptItems": null }`. `receiptDate` do hệ thống gán.

Hạn chế đã kiểm chứng: PUT phải gửi **cả `receiptDate`**, nếu thiếu trả 500; không tạo được dòng chi tiết (`inventoryReceiptItems`) qua API này; chuyển phiếu sang `APPROVED` **không** cộng tồn kho.

## 12. Dashboard (Admin)

### 12.1. `GET /api/dashboard/kpi`

```json
{
  "processingOrders": 3,
  "todayRevenue": 1355000.0,
  "topProducts": [ { "productId": "e7d8a664-...", "productName": "Áo thun cotton basic", "quantity": 2 } ],
  "topCustomers": [ { "customerId": "9ddf56f3-...", "email": "user@shopease.test", "total": 857000.0 } ],
  "timestamp": 1791839863982
}
```

`processingOrders` là số đơn `PENDING` + `SHIPPING` + `WAIT_DELIVER`; `todayRevenue` hiện là tổng mọi đơn `PAID` (chưa giới hạn trong ngày); `timestamp` là mili giây epoch.

### 12.2. `POST /api/dashboard/refresh`

Đẩy lại biểu đồ qua WebSocket. Phản hồi 200 text: `Dashboard refreshed`.

### 12.3. WebSocket `/ws/dashboard` (không cần token)

Máy chủ gửi từng khung văn bản JSON:

```json
{ "type": "KPI_UPDATE", "payload": { "...": "..." }, "timestamp": "2026-10-13T04:11:03.316Z" }
```

Khi kết nối nhận ngay `CONNECTION_SUCCESS`; sau đó cứ khoảng 5 giây nhận cập nhật, và khi có sự kiện. Các `type`: `KPI_UPDATE`, `NEW_ORDER`, `ORDER_STATUS_CHANGED`, `REVENUE_UPDATED`, `HOURLY_REVENUE`, `ORDER_STATUS_DISTRIBUTION`, `TOP_PRODUCTS`, `TOP_CUSTOMERS`.

## 13. Hệ thống

| Endpoint | Mô tả | Phản hồi |
| --- | --- | --- |
| `GET /api/health` | Kiểm tra dịch vụ (Public) | `{ "message": "Service is running", "status": "UP", "timestamp": "2026-10-13T04:11:03.316" }` |
| `GET /uploads/**` | Tải ảnh lưu cục bộ (Public) | File ảnh |
| `GET /swagger-ui.html` | Giao diện Swagger (Public) | Trang HTML |
| `GET /v3/api-docs` | Đặc tả OpenAPI JSON (Public) | JSON (đã kiểm chứng 200) |

## 14. Kết quả kiểm thử

### 14.1. Tóm tắt

Toàn bộ API được gọi thật qua bộ test tích hợp `ApiFlowIT` (đi qua Security filter, controller, service và cơ sở dữ liệu Neon thật; không mở cổng mạng). Dữ liệu tạo ra có tiền tố `apitest-` và được dọn sạch sau mỗi lần chạy. Lần chạy cuối: **122 kiểm tra, 104 đạt, 18 lệch kỳ vọng (đều đã được xác định nguyên nhân), 0 lỗi ngoài dự kiến.**

| Nhóm kiểm tra | Số kiểm tra | Đạt | Lệch kỳ vọng |
| --- | --- | --- | --- |
| A. Công khai | 11 | 11 | 0 |
| B. Xác thực | 10 | 10 | 0 |
| C. Phân quyền | 6 | 6 | 0 |
| D. Quản trị người dùng | 7 | 7 | 0 |
| E. Danh mục và sản phẩm | 22 | 19 | 3 |
| F. Địa chỉ | 7 | 7 | 0 |
| G. Giỏ hàng | 6 | 5 | 1 |
| H. Đơn hàng | 24 | 18 | 6 |
| I. Thanh toán VNPAY | 5 | 4 | 1 |
| J. Đánh giá | 9 | 6 | 3 |
| K. Kho | 7 | 4 | 3 |
| L. Dashboard | 3 | 3 | 0 |
| M. Xóa dữ liệu | 5 | 4 | 1 |

Các chức năng đã kiểm chứng hoạt động đúng:

- Đăng ký, chặn trùng email, chặn mật khẩu ngắn, kích hoạt bằng mã, đăng nhập, chặn đăng nhập khi chưa kích hoạt hoặc sai mật khẩu, hồ sơ.
- Phân quyền: chưa đăng nhập hoặc token rác trả 401; USER gọi API quản trị trả 403; ADMIN gọi được.
- Quản trị người dùng: tạo, sửa, đổi vai trò, chặn vai trò sai (400), vô hiệu hóa, 404 khi không tồn tại.
- Danh mục, loại danh mục, sản phẩm kèm biến thể: tạo, xem, sửa; lọc theo danh mục và slug; tìm kiếm có phân trang.
- Ảnh sản phẩm: tải lên, liệt kê, đặt ảnh chính, xóa.
- Địa chỉ: thêm, sửa, xóa, chỉ chủ sở hữu được sửa hoặc xóa (người khác nhận 403).
- Giỏ hàng: thêm, xem, sửa, xóa.
- Đơn hàng: tạo, xem đơn của tôi, chi tiết, danh sách chưa đánh giá; ADMIN xem tất cả; luồng `PENDING → SHIPPING → WAIT_DELIVER → PAID`; chặn chuyển sai luồng và chặn chuyển tiếp từ trạng thái cuối; ghi đủ lịch sử trạng thái.
- VNPAY: tạo URL thanh toán, xử lý callback và đặt trạng thái thanh toán.
- Đánh giá: tạo, xem, sửa, xóa; điểm trung bình sản phẩm cập nhật đúng.
- Phiếu nhập kho: tạo (không dòng chi tiết), xem, xóa. Dashboard và Swagger hoạt động.

### 14.2. Các điểm chưa đúng phát hiện được

Mức độ: Cao ảnh hưởng tiền, dữ liệu hoặc quyền; Trung bình là chức năng hỏng hoặc lỗi trả sai mã; Thấp là điểm cần cải thiện.

| Mã | Mức | Vấn đề | Bằng chứng (mã kiểm tra) | Đề xuất |
| --- | --- | --- | --- | --- |
| F-01 | Cao | Tạo đơn không trừ tồn kho; hủy đơn lại cộng trả kho nên tồn kho tăng sai (tồn 5 thành 8 sau khi tạo và hủy đơn 3 sản phẩm). | H2, H21 | Trừ kho trong một giao dịch khi tạo đơn, ghi `stock_movements`. |
| F-02 | Cao | Callback VNPAY không kiểm tra chữ ký: gọi giả mạo vẫn chuyển hướng `status=success` và đặt đơn đã thanh toán. | I5 | Kiểm tra `vnp_SecureHash` và số tiền, thêm IPN. |
| F-03 | Cao | Mọi người dùng đăng nhập đổi được trạng thái thanh toán của đơn bất kỳ thành PAID. | H19 | Giới hạn cho ADMIN hoặc chỉ cho hệ thống VNPAY. |
| F-04 | Cao | Người dùng xem được đơn của người khác (200); lệnh xóa đơn của người khác không bị chặn quyền. | H9, H24 | Kiểm tra chủ đơn (lấy người dùng từ token). |
| F-05 | Trung bình | API công khai `GET /api/reviews/product/<built-in function id>` trả email và số điện thoại người đánh giá. | J2b | Chỉ trả tên hiển thị. |
| F-06 | Trung bình | Không tạo được biến thể riêng lẻ: `POST /api/product-variants` luôn lỗi 500 vì `product` không đọc được từ JSON (PUT, DELETE của biến thể mới tạo vì thế cũng không kiểm thử được). | E20, E21, E22 | Dùng request riêng có `productId`. |
| F-07 | Trung bình | Phiếu nhập kho: không tạo được dòng chi tiết (500); PUT bắt buộc gửi `receiptDate` (500 nếu thiếu); duyệt phiếu không cộng tồn kho. | K2, K5, K6 | Dùng DTO riêng, cộng kho khi `APPROVED`. |
| F-08 | Trung bình | Lỗi nghiệp vụ trả 500 thay vì 4xx: trùng `orderNumber`, đánh giá sai quyền, `rating` ngoài 1 đến 5, xóa loại danh mục còn sản phẩm, xóa đơn đã có lịch sử trạng thái. | H18, J6, J7, M3, H24 | Thêm validation và ngoại lệ riêng (400, 403, 409). |
| F-09 | Thấp | Giỏ hàng không kiểm tra số lượng so với tồn kho (thêm 9999 vẫn được). | G5 | Kiểm tra tồn kho khi thêm giỏ. |
| F-10 | Thấp | `GET /api/orders/me`, `/{id}`, `/` không trả `paymentStatus`; chỉ POST và PATCH mới có. | H3, H4 | Thêm `paymentStatus` vào `OrderResponse`. |
| F-11 | Thấp | Tìm kiếm theo `keyword` phân biệt dấu: `ao` không khớp `Áo`. | A3 | Chuẩn hóa không dấu khi tìm. |
| F-12 | Thấp | `PUT /api/products/{id}` ghi đè toàn bộ; trường thiếu thành `null` (suy ra từ mã nguồn, ví dụ `rating`). | E12 | Chỉ cập nhật trường được gửi. |

Đã sửa trong lúc kiểm thử: phản hồi 401 và 403 trước đây trả `timestamp` dạng mảng số `[2026,10,13,...]`; nay trả chuỗi ISO giống các lỗi khác (`WebSecurityConfig`).

### 14.3. Cách chạy lại bộ kiểm thử

```bash
./mvnw test -Dtest=ApiFlowIT
```

Cần file `.env` (kết nối cơ sở dữ liệu và `JWT_KEY`). Kết quả từng lời gọi (request, response, mã trả về) được ghi vào `target/api-results.json`. Bộ test không chạy trong `./mvnw test` mặc định vì tên kết thúc bằng `IT`.

