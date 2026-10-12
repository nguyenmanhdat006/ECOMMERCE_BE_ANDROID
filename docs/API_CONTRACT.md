# API Contract – ShopEase Backend

Phiên bản 1.1 – 13/10/2026 (đã cập nhật sau khi khắc phục các lỗi phát hiện ở bản 1.0). Nội dung được lập từ mã nguồn và **đã kiểm chứng bằng cách gọi thật từng API** (bộ test `ApiFlowIT`, xem mục 14). Các ví dụ phản hồi là kết quả thật; riêng id, email, token đã rút gọn.

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
| 400 | Dữ liệu sai hoặc thiếu (kiểm tra đầu vào), sai kiểu (ví dụ UUID sai định dạng), chuyển trạng thái đơn sai luồng, thao tác không hợp lệ với trạng thái hiện tại |
| 401 | Chưa đăng nhập, token sai hoặc hết hạn, sai email hoặc mật khẩu |
| 403 | Đã đăng nhập nhưng không đủ quyền |
| 404 | Không tìm thấy dữ liệu |
| 409 | Xung đột dữ liệu: trùng mã (đơn, phiếu, slug), không đủ tồn kho, đối tượng đang được tham chiếu nên không xóa được |
| 500 | Lỗi máy chủ không lường trước |

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

Xóa loại còn sản phẩm tham chiếu trả **409**.

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
| `keyword` | string | – | Tìm trong tên, mô tả, thương hiệu; không phân biệt hoa thường và dấu tiếng Việt (`ao` khớp `Áo`, `dep` khớp `Đẹp`) |
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

| Method | Endpoint | Mô tả | Mã |
| --- | --- | --- | --- |
| POST | `/api/products` | Tạo sản phẩm (kèm biến thể) | 201 |
| PUT | `/api/products/{id}` | Cập nhật một phần | 200 |
| DELETE | `/api/products/{id}` | Xóa sản phẩm chưa phát sinh đơn hàng, phiếu nhập, biến động kho | 204 |

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
| `newArrival` | boolean | Không | Hàng mới (mặc định `false`) |
| `variants` | mảng | Không | Mỗi phần tử: `color`, `size`, `stockQuantity` (số nguyên ≥ 0) |
| `productResources` | mảng | Không | Mỗi phần tử: `name`, `url`, `type`, `isPrimary` |

```json
{
  "name": "Áo thun cotton", "description": "Áo thun 100% cotton", "price": 199000, "brand": "Coolmate",
  "newArrival": true, "slug": "ao-thun-cotton", "categoryId": "48ac3693-...", "categoryTypeId": "c966b496-...",
  "variants": [ { "color": "Đen", "size": "M", "stockQuantity": 10 }, { "color": "Đen", "size": "L", "stockQuantity": 5 } ]
}
```

Phản hồi 201 và 200 là **`ProductDto` đầy đủ** như mục 5.1 (có `categoryName`, `variants`, `productResources`, `newArrival`).

Lỗi: 400 thiếu trường bắt buộc hoặc giá âm; 404 danh mục hoặc loại không tồn tại; 409 trùng `slug`.

**PUT cập nhật một phần**: chỉ ghi các trường có gửi lên (`name`, `description`, `price`, `brand`, `slug`, `newArrival`, `categoryId`, `categoryTypeId`); trường không gửi được giữ nguyên. `variants`, `productResources` và `rating` không được sửa qua PUT (biến thể và ảnh có API riêng, `rating` do hệ thống tính từ đánh giá).

```json
{ "price": 259000 }
```

**DELETE** là xóa cứng kèm biến thể, ảnh, dòng giỏ hàng. Sản phẩm đã có đơn hàng, phiếu nhập hoặc biến động kho trả **409** (không xóa để tránh mất lịch sử).

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

| Method | Endpoint | Quyền | Mã |
| --- | --- | --- | --- |
| GET | `/api/product-variants`, `/api/product-variants/{id}` | User | 200 |
| POST | `/api/product-variants` | Admin | 201 |
| PUT | `/api/product-variants/{id}` | Admin | 200 |
| DELETE | `/api/product-variants/{id}` | Admin | 200 (thân rỗng) |

Đối tượng trả về (`ProductVariantResponse`):

```json
{ "id": "f26cc957-...", "productId": "7c710c58-...", "color": "Den", "size": "M", "stockQuantity": 10 }
```

Request tạo (sửa dùng cùng cấu trúc, `productId` được bỏ qua):

| Trường | Kiểu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `productId` | uuid | Có (khi tạo) | Sản phẩm đã tồn tại |
| `color` | string | Có | Màu |
| `size` | string | Có | Kích cỡ |
| `stockQuantity` | số nguyên | Có | Tồn kho, ≥ 0 |

Lỗi: 400 thiếu hoặc sai trường; 404 sản phẩm hoặc biến thể không tồn tại; 403 nếu không phải Admin; **409** khi xóa biến thể đã phát sinh đơn hàng, phiếu nhập hoặc biến động kho.

Tồn kho thay đổi tự động theo đơn hàng và phiếu nhập (mục 8 và 11); `PUT` chỉ nên dùng để điều chỉnh thủ công.

## 6. Địa chỉ (`/api/addresses`)

Đối tượng địa chỉ: `id`, `name`, `street`, `city`, `state`, `zipCode`, `phoneNumber` (phản hồi danh sách có thêm `userId`).

| Method | Endpoint | Quyền | Mô tả | Mã |
| --- | --- | --- | --- | --- |
| GET | `/api/addresses/my-addresses` | User | Địa chỉ của tôi | 200 |
| POST | `/api/addresses` | User | Thêm địa chỉ cho người đang đăng nhập | 201 |
| PUT | `/api/addresses/{id}` | Chủ sở hữu | Sửa; người khác nhận **403** thân rỗng | 200 |
| DELETE | `/api/addresses/{id}` | Chủ sở hữu | Xóa; người khác **403** | 200 (rỗng) |
| GET | `/api/addresses/{id}` | Chủ sở hữu hoặc Admin | Chi tiết một địa chỉ; người khác **403** | 200 |
| GET | `/api/addresses/user/{userId}` | Chính user đó hoặc Admin | Địa chỉ của một user; người khác **403**; 404 nếu user không tồn tại | 200 |
| GET | `/api/addresses` | **Admin** | Tất cả địa chỉ | 200 |

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

## 7. Giỏ hàng (`/api/carts`)

Đối tượng `CartDto`: `id`, `quantity`, `createdAt`, `updatedAt`, `userId`, `productId`, `productVariantId`.

| Method | Endpoint | Quyền | Mô tả | Mã |
| --- | --- | --- | --- | --- |
| POST | `/api/carts` | User | Thêm vào giỏ của **người đang đăng nhập** | 201 |
| PUT | `/api/carts/{id}` | Chủ giỏ hoặc Admin | Đặt lại số lượng | 200 |
| GET | `/api/carts/{id}` | Chủ giỏ hoặc Admin | Một dòng giỏ (`CartDto`) | 200 |
| GET | `/api/carts/user/{userId}` | Chủ giỏ hoặc Admin | Giỏ của một user kèm thông tin sản phẩm | 200 |
| GET | `/api/carts` | **Admin** | Tất cả dòng giỏ | 200 |
| DELETE | `/api/carts/{id}` | Chủ giỏ hoặc Admin | Xóa dòng giỏ | 200 (rỗng) |

Người không phải chủ giỏ (và không phải Admin) nhận **403**.

Request thêm/sửa:

| Trường | Kiểu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `productVariantId` | uuid | Có | Biến thể muốn mua; sản phẩm được suy ra từ biến thể |
| `quantity` | số nguyên | Có | Lớn hơn 0 |
| `productId` | uuid | Không | Nếu gửi phải khớp với biến thể, nếu không trả 400 |

```json
{ "productVariantId": "f26cc957-...", "quantity": 2 }
```

Quy tắc: `userId` luôn lấy từ token. Thêm lại cùng một biến thể thì **cộng dồn** số lượng vào dòng đã có. Tổng số lượng vượt tồn kho trả **400** (`"Số lượng vượt quá tồn kho (hiện còn N)"`).

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

## 8. Đơn hàng (`/api/orders`)

### 8.1. Bảng endpoint

| Method | Endpoint | Quyền | Mô tả | Mã |
| --- | --- | --- | --- | --- |
| POST | `/api/orders` | User | Tạo đơn (người đặt là người đang đăng nhập) | 201 |
| GET | `/api/orders/me` | User | Đơn của tôi (theo token) | 200 |
| GET | `/api/orders/unreviewed` | User | Chi tiết đơn đã mua chưa đánh giá | 200 |
| GET | `/api/orders/{id}` | Chủ đơn hoặc Admin | Chi tiết đơn; người khác **403**; 404 nếu không có | 200 |
| GET | `/api/orders` | **Admin** | Tất cả đơn | 200 |
| PATCH | `/api/orders/{id}/status` | **Admin** | Đổi trạng thái đơn | 200 |
| PATCH | `/api/orders/{id}/payment-status` | **Admin** | Đổi trạng thái thanh toán | 200 |
| DELETE | `/api/orders/{id}` | **Admin** | Xóa đơn đã hủy hoặc đã hoàn trả | 200 (rỗng) |

### 8.2. `POST /api/orders` – Tạo đơn

| Trường | Kiểu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `shippingAddress` | string | Có | Địa chỉ giao dạng chuỗi |
| `orderItems` | mảng | Có | Ít nhất một dòng |
| `orderItems[].productVariantId` | uuid | Có | Biến thể muốn mua |
| `orderItems[].quantity` | số nguyên | Có | Lớn hơn 0 |
| `orderItems[].productId` | uuid | Không | Nếu gửi phải khớp với biến thể |
| `paymentMethod` | string | Không | `COD` (mặc định) hoặc `VNPAY`; giá trị khác trả 400 |
| `orderNumber` | string | Không | Mã đơn; bỏ trống thì máy chủ tự sinh (`SE-yyyyMMddHHmmss-XXXX`); **trùng trả 409** |
| `notes` | string | Không | Ghi chú |

Các trường `customerId`, `status`, `totalAmount`, `orderItems[].unitPrice`, `orderItems[].totalPrice` nếu gửi sẽ **bị bỏ qua**: người đặt lấy từ token, trạng thái luôn là `PENDING`, đơn giá lấy từ giá sản phẩm, thành tiền và tổng tiền do máy chủ tính.

```json
{
  "orderNumber": "SE-20261013-001", "paymentMethod": "COD",
  "shippingAddress": "Nguyen Van A - 0911222333 - 12 Nguyễn Trãi, Quận 1, HCM", "notes": "Giao giờ hành chính",
  "orderItems": [ { "productVariantId": "f26cc957-...", "quantity": 2 } ]
}
```

Phản hồi 201:

```json
{
  "id": "f69d7dbd-...", "orderNumber": "SE-20261013-001", "orderDate": "2026-10-12T21:11:35.107+00:00",
  "totalAmount": 518000, "status": "PENDING", "paymentStatus": "UNPAID", "paymentMethod": "COD",
  "shippingAddress": "...", "notes": "Giao giờ hành chính",
  "orderItems": [ { "id": "b5b8ca9a-...", "quantity": 2, "unitPrice": 259000, "totalPrice": 518000, "isReviewed": false } ],
  "updatedAt": null
}
```

Quy tắc: toàn bộ đơn được xử lý trong một giao dịch. **Tồn kho bị trừ ngay** khi tạo đơn và mỗi lần trừ được ghi vào sổ biến động kho (`stock_movements`, loại `OUT`). Không đủ hàng ở bất kỳ dòng nào thì cả đơn bị từ chối với **409** và tồn kho giữ nguyên. Lỗi khác: 400 (không có sản phẩm, thiếu địa chỉ, số lượng ≤ 0, `paymentMethod` sai, biến thể không thuộc `productId`), 404 (biến thể không tồn tại).

### 8.3. `GET /api/orders/me`, `GET /api/orders/{id}`, `GET /api/orders` – Dạng `OrderResponse`

```json
{
  "id": "f69d7dbd-...", "orderNumber": "SE-20261013-001", "orderDate": "2026-10-12T21:11:35.107+00:00",
  "totalAmount": 518000.00, "status": "PENDING", "paymentStatus": "UNPAID", "paymentMethod": "COD",
  "shippingAddress": "...", "notes": "...", "customerId": "bdf369c9-...", "customerName": "Lan Nguyen",
  "orderItems": [ {
    "id": "b5b8ca9a-...", "quantity": 2, "unitPrice": 259000.00, "totalPrice": 518000.00,
    "productId": "7c710c58-...", "productVariantId": "f26cc957-...", "productName": "Ao test",
    "isReviewed": false, "thumbnail": "http://localhost:8080/uploads/..."
  } ]
}
```

`GET /api/orders/unreviewed` trả mảng `OrderItemResponse` (cùng cấu trúc phần tử `orderItems` ở trên).

### 8.4. `PATCH /api/orders/{id}/status` – Đổi trạng thái (Admin)

Body:

```json
{ "status": "SHIPPING" }
```

Người thực hiện được ghi vào lịch sử lấy từ **token** của Admin (tham số query `changedBy` không còn bắt buộc và bị bỏ qua). Giá trị `status`: `PENDING`, `SHIPPING`, `WAIT_DELIVER`, `PAID`, `CANCELED`, `REFUND`. Luồng hợp lệ:

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

Giá trị `status` sai tên trả 400. Chuyển sang `CANCELED` hoặc `REFUND` **cộng trả đúng số lượng đã trừ** vào kho từng biến thể (ghi sổ loại `IN`). Mỗi lần đổi ghi một dòng `order_status_history`.

### 8.5. `PATCH /api/orders/{id}/payment-status` – Trạng thái thanh toán (Admin)

Body `{ "status": "PAID" }` với `status` thuộc `UNPAID`, `PAID`, `FAILED`, `REFUND`. Phản hồi 200: entity đơn (có `paymentStatus`). Người dùng thường nhận **403**; khách thanh toán qua VNPAY thì trạng thái được cập nhật tự động (mục 9).

### 8.6. `DELETE /api/orders/{id}` (Admin)

Chỉ xóa được đơn ở trạng thái `CANCELED` hoặc `REFUND` (xóa kèm lịch sử trạng thái); đơn ở trạng thái khác trả **400**. Người dùng thường nhận **403**.

## 9. Thanh toán VNPAY

### 9.1. `POST /api/vnpay/create-payment` (User)

| Trường | Kiểu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `orderId` | uuid | Có | Đơn của chính người đang đăng nhập (Admin được phép với mọi đơn) |

```json
{ "orderId": "ccf79480-..." }
```

Số tiền **lấy từ tổng tiền của đơn** (trường `amount` nếu gửi sẽ bị bỏ qua). Phản hồi 200 (text thuần, URL đã ký HMAC-SHA512):

```text
https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=25900000&vnp_Command=pay&vnp_CreateDate=20261013041151&vnp_CurrCode=VND&...&vnp_SecureHash=...
```

Lỗi: 404 đơn không tồn tại; 403 đơn của người khác; 409 đơn đã thanh toán; 400 đơn đã hủy hoặc hoàn trả, hoặc chưa cấu hình `VNPAY_TMN_CODE` và `VNPAY_HASH_SECRET`; 401 chưa đăng nhập. `vnp_ReturnUrl` trỏ về frontend (`VNPAY_RETURN_URL_BASE/vnpay-done/{orderId}`).

### 9.2. `GET /api/vnpay/return` (Public)

Frontend gọi lại endpoint này với toàn bộ tham số VNPAY trả về (`vnp_TxnRef`, `vnp_Amount`, `vnp_ResponseCode`, `vnp_BankCode`, `vnp_CardType`, `vnp_OrderInfo`, `vnp_PayDate`, `vnp_TransactionNo`, `vnp_SecureHash`...). Kết quả luôn là **302** về frontend:

```text
http://localhost:5173/vnpay-done?orderId=<id>&vnp_Amount=...&vnp_BankCode=...&...&status=success
```

Máy chủ **kiểm tra chữ ký `vnp_SecureHash`** và đối chiếu `vnp_Amount` với tổng tiền đơn × 100:

| Tình huống | Kết quả |
| --- | --- |
| Chữ ký đúng, số tiền khớp, `vnp_ResponseCode = 00` | Đơn `payment_status = PAID`, `status=success` |
| Chữ ký đúng, số tiền khớp, mã khác `00` | Đơn `payment_status = FAILED`, `status=fail` |
| Thiếu chữ ký, sai chữ ký, sai số tiền, đơn không tồn tại | **Không thay đổi dữ liệu**, `status=fail` |
| Đơn đã `PAID` | Không đổi trạng thái (gọi lại an toàn) |

Tham số thiếu được coi là chuỗi rỗng, không gây lỗi.

## 10. Đánh giá (`/api/reviews`)

| Method | Endpoint | Quyền | Mô tả | Mã |
| --- | --- | --- | --- | --- |
| POST | `/api/reviews` | User | Viết đánh giá (người viết là người đang đăng nhập) | 201 |
| GET | `/api/reviews/product/{productId}` | Public | Đánh giá của sản phẩm | 200 |
| PUT | `/api/reviews/{id}` | Chủ đánh giá | Sửa điểm và nhận xét | 200 |
| DELETE | `/api/reviews/{id}` | Chủ đánh giá hoặc Admin | Xóa đánh giá | 200 |

Request tạo:

| Trường | Kiểu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `productId` | uuid | Có | Sản phẩm, phải khớp với dòng đơn |
| `orderItemId` | uuid | Có | Dòng đơn đã mua, thuộc đơn của chính người viết |
| `rating` | số nguyên | Có | 1 đến 5 |
| `comment` | string | Không | Nhận xét |

```json
{ "productId": "7c710c58-...", "orderItemId": "b5b8ca9a-...", "rating": 5, "comment": "Vải mát, giao nhanh" }
```

Request sửa (`PUT`): `{ "rating": 4, "comment": "Đã sửa" }`.

Phản hồi POST (201) và PUT (200):

```json
{ "id": "9d300bf9-...", "rating": 5, "comment": "Vải mát, giao nhanh", "createdAt": "...", "updatedAt": "...",
  "user": { "id": "bdf369c9-...", "firstName": "Lan", "lastName": "Nguyen", "avatar": null } }
```

Phản hồi `GET /api/reviews/product/{productId}`: mảng các đối tượng trên. Trường `user` chỉ có `id`, `firstName`, `lastName`, `avatar` (**không có email, số điện thoại**).

Quy tắc và lỗi:

- Chỉ đánh giá được dòng đơn thuộc đơn của mình (khác: **403**) và đơn đã hoàn thành `PAID` (khác: **400**).
- Mỗi dòng đơn một đánh giá; đánh giá lần hai trả **409**.
- `rating` ngoài 1 đến 5 trả **400**; `productId` không khớp dòng đơn trả **400**.
- Sửa hoặc xóa đánh giá của người khác trả **403** (Admin được xóa).
- Sau khi tạo, `order_item.is_reviewed = true` và `product.rating` được tính lại (đánh giá 5 sao thì rating = 5.0); xóa đánh giá thì dòng đơn được đánh giá lại và rating tính lại.

## 11. Phiếu nhập kho (`/api/inventory-receipts`, Admin)

| Method | Endpoint | Mô tả | Mã |
| --- | --- | --- | --- |
| GET | `/api/inventory-receipts` | Danh sách phiếu (kèm dòng chi tiết) | 200 |
| GET | `/api/inventory-receipts/{id}` | Chi tiết phiếu | 200 |
| POST | `/api/inventory-receipts` | Tạo phiếu | 201 |
| PUT | `/api/inventory-receipts/{id}` | Sửa phiếu đang chờ, hoặc duyệt/hủy phiếu qua `status` | 200 |
| DELETE | `/api/inventory-receipts/{id}` | Xóa phiếu `PENDING` hoặc `CANCELLED` | 200 (rỗng) |

Request tạo và sửa:

| Trường | Kiểu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `receiptNumber` | string | Có | Số phiếu, duy nhất (trùng trả 409) |
| `supplierName` | string | Có | Nhà cung cấp |
| `items` | mảng | Có | Ít nhất một dòng |
| `items[].productVariantId` | uuid | Có | Biến thể nhập; sản phẩm suy ra từ biến thể |
| `items[].quantity` | số nguyên | Có | Lớn hơn 0 |
| `items[].unitCost` | số | Có | Giá vốn một đơn vị, không âm |
| `status` | string | Không | `PENDING` (mặc định) hoặc `APPROVED` khi tạo; khi sửa có thể đổi sang `APPROVED` hoặc `CANCELLED` |
| `notes` | string | Không | Ghi chú |

Ngày nhập và tổng tiền do máy chủ tính.

```json
{
  "receiptNumber": "PN-001", "supplierName": "Công ty May Việt Tiến", "status": "PENDING", "notes": "Phiếu test",
  "items": [ { "productVariantId": "f26cc957-...", "quantity": 5, "unitCost": 100 } ]
}
```

Phản hồi 201 và 200:

```json
{
  "id": "92dc3ce9-...", "receiptNumber": "PN-001", "supplierName": "Công ty May Việt Tiến",
  "receiptDate": "2026-10-12T21:11:55.676+00:00", "totalAmount": 500, "status": "PENDING", "notes": "Phiếu test",
  "items": [ { "id": "...", "productId": "7c710c58-...", "productVariantId": "f26cc957-...", "productName": "Ao test",
               "color": "Den", "size": "M", "quantity": 5, "unitCost": 100, "totalCost": 500 } ]
}
```

Quy tắc:

- **Duyệt phiếu** (`PENDING` sang `APPROVED`, hoặc tạo thẳng với `APPROVED`) **cộng số lượng vào tồn kho** từng biến thể và ghi sổ biến động kho loại `IN`.
- `APPROVED` và `CANCELLED` là trạng thái cuối: sửa phiếu đã duyệt hoặc đã hủy trả **400**; xóa phiếu đã duyệt trả **400**.
- Sửa phiếu `PENDING` thay toàn bộ dòng chi tiết bằng danh sách `items` gửi lên.
- Tạo phiếu không có dòng chi tiết, hoặc `status` ngoài `PENDING`, `APPROVED`, `CANCELLED` trả **400**.

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

Toàn bộ API được gọi thật qua bộ test tích hợp `ApiFlowIT` (đi qua Security filter, controller, service và cơ sở dữ liệu Neon thật; không mở cổng mạng). Dữ liệu tạo ra có tiền tố `apitest-` và được dọn sạch sau mỗi lần chạy. Lần chạy cuối: **174 kiểm tra, 174 đạt**.

| Nhóm kiểm tra | Số kiểm tra | Đạt | Không đạt |
| --- | --- | --- | --- |
| A. Công khai | 12 | 12 | 0 |
| B. Xác thực | 10 | 10 | 0 |
| C. Phân quyền | 8 | 8 | 0 |
| D. Quản trị người dùng | 7 | 7 | 0 |
| E. Danh mục và sản phẩm | 29 | 29 | 0 |
| F. Địa chỉ | 9 | 9 | 0 |
| G. Giỏ hàng | 11 | 11 | 0 |
| H. Đơn hàng | 37 | 37 | 0 |
| I. Thanh toán VNPAY | 15 | 15 | 0 |
| J. Đánh giá | 13 | 13 | 0 |
| K. Kho | 13 | 13 | 0 |
| L. Dashboard | 3 | 3 | 0 |
| M. Xóa dữ liệu | 7 | 7 | 0 |

Các chức năng đã kiểm chứng hoạt động đúng:

- Đăng ký, chặn trùng email, chặn mật khẩu ngắn, kích hoạt bằng mã, đăng nhập, chặn đăng nhập khi chưa kích hoạt hoặc sai mật khẩu, hồ sơ.
- Phân quyền: chưa đăng nhập hoặc token rác trả 401; USER gọi API quản trị trả 403; ADMIN gọi được; mỗi người chỉ truy cập được giỏ hàng, địa chỉ, đơn hàng, đánh giá của chính mình.
- Quản trị người dùng, danh mục, loại danh mục, sản phẩm kèm biến thể, ảnh sản phẩm, biến thể riêng lẻ.
- Tìm kiếm có phân trang, lọc, và tìm không phân biệt dấu tiếng Việt.
- Giỏ hàng có kiểm tra tồn kho; đơn hàng tính giá phía máy chủ, trừ kho, từ chối khi hết hàng, hủy đơn cộng trả đúng số lượng, xóa đơn an toàn.
- Luồng trạng thái đơn, chặn chuyển sai luồng, ghi lịch sử với người thực hiện lấy từ token.
- VNPAY: URL có chữ ký, callback hợp lệ cập nhật đơn; callback giả mạo, sai số tiền, thiếu tham số không làm đổi dữ liệu.
- Đánh giá: tạo, sửa, xóa, chặn đánh giá sai quyền hoặc lặp, cập nhật điểm trung bình, không lộ email và số điện thoại.
- Phiếu nhập kho: tạo kèm dòng chi tiết, duyệt phiếu cộng tồn kho và ghi sổ biến động, chặn sửa hoặc xóa phiếu đã duyệt.
- Dashboard, Swagger, định dạng lỗi thống nhất.

### 14.2. Các điểm đã khắc phục sau lần kiểm thử đầu

Lần kiểm thử đầu (122 kiểm tra) phát hiện 12 vấn đề; tất cả đã được sửa và kiểm chứng lại.

| Mã | Mức | Vấn đề ban đầu | Cách khắc phục |
| --- | --- | --- | --- |
| F-01 | Cao | Tạo đơn không trừ kho, hủy đơn cộng kho làm tồn tăng sai | Trừ kho trong giao dịch khi tạo đơn (khóa dòng biến thể, ghi sổ biến động); hết hàng trả 409; hủy hoặc hoàn trả cộng đúng số đã trừ |
| F-02 | Cao | Callback VNPAY không kiểm tra chữ ký | Kiểm tra `vnp_SecureHash`, đối chiếu số tiền; không hợp lệ thì không đổi dữ liệu; số tiền tạo URL lấy từ đơn |
| F-03 | Cao | Mọi user đổi được trạng thái thanh toán | Chỉ Admin được gọi `payment-status` |
| F-04 | Cao | Xem và xóa đơn của người khác; xóa đơn có lịch sử lỗi 500 | Xem đơn chỉ chủ đơn hoặc Admin; xóa chỉ Admin, chỉ đơn đã hủy hoặc hoàn trả, xóa kèm lịch sử |
| F-05 | Trung bình | API công khai lộ email và số điện thoại người đánh giá | `user` chỉ còn `id`, `firstName`, `lastName`, `avatar` |
| F-06 | Trung bình | Không tạo được biến thể riêng lẻ | `POST /api/product-variants` dùng request có `productId`; trả DTO gọn |
| F-07 | Trung bình | Phiếu nhập: không tạo được dòng chi tiết, PUT bắt buộc `receiptDate`, không cộng kho | Request riêng có `items`; máy chủ tính tiền và ngày; duyệt phiếu cộng kho; trạng thái cuối bất biến |
| F-08 | Trung bình | Lỗi nghiệp vụ trả 500 | Thêm 409 (trùng, hết hàng, đang được tham chiếu), 403 (sai quyền), 400 (kiểm tra đầu vào) |
| F-09 | Thấp | Giỏ hàng không kiểm tra tồn kho | Số lượng vượt tồn kho trả 400; thêm lại cùng biến thể thì cộng dồn |
| F-10 | Thấp | `OrderResponse` không có `paymentStatus` | Đã thêm |
| F-11 | Thấp | Tìm kiếm phân biệt dấu | Dùng `unaccent` của PostgreSQL (migration V3) |
| F-12 | Thấp | `PUT /api/products/{id}` ghi đè trường thiếu thành `null` | Cập nhật một phần; `rating`, biến thể, ảnh không bị ảnh hưởng |

Khắc phục thêm cùng đợt: `userId` không còn do client gửi (đơn, giỏ, đánh giá lấy từ token); chỉ chủ sở hữu hoặc Admin truy cập giỏ hàng, địa chỉ; sửa đánh giá của người khác bị chặn (trước đây không kiểm tra); đánh giá chỉ khi đơn hoàn thành và mỗi dòng đơn một đánh giá; không xóa được sản phẩm hoặc biến thể đã phát sinh đơn hàng.

Lưu ý khi chạy kiểm thử: kết nối tới Neon thỉnh thoảng báo lỗi I/O chập chờn (ví dụ `An I/O error occurred while sending to the backend`); chạy lại sẽ đạt.

### 14.3. Cách chạy lại bộ kiểm thử

```bash
./mvnw test -Dtest=ApiFlowIT
```

Cần file `.env` (kết nối cơ sở dữ liệu và `JWT_KEY`). Kết quả từng lời gọi (request, response, mã trả về) được ghi vào `target/api-results.json`. Bộ test không chạy trong `./mvnw test` mặc định vì tên kết thúc bằng `IT`.
