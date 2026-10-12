-- V2: dọn bảng của các tính năng đã bỏ, thêm ràng buộc dữ liệu và index cho khóa ngoại.

-- Tính năng đã xóa khỏi code (chat, lịch sử mua, lịch sử trạng thái sản phẩm)
DROP TABLE IF EXISTS chat_message;
DROP TABLE IF EXISTS purchase_histories;
DROP TABLE IF EXISTS product_statuses;

-- Role: mã role duy nhất; mỗi user chỉ gán một role một lần
ALTER TABLE auth_authority ADD CONSTRAINT uq_auth_authority_role_code UNIQUE (role_code);
ALTER TABLE auth_user_authority ADD CONSTRAINT pk_auth_user_authority PRIMARY KEY (user_id, authorities_id);

-- Giá trị hợp lệ
ALTER TABLE reviews ADD CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5);
ALTER TABLE products ADD CONSTRAINT ck_products_price CHECK (price >= 0);
ALTER TABLE product_variant ADD CONSTRAINT ck_product_variant_stock CHECK (stock_quantity >= 0);
ALTER TABLE carts ADD CONSTRAINT ck_carts_quantity CHECK (quantity > 0);
ALTER TABLE order_items ADD CONSTRAINT ck_order_items_quantity CHECK (quantity > 0);
ALTER TABLE order_items ADD CONSTRAINT ck_order_items_prices CHECK (unit_price >= 0 AND total_price >= 0);
ALTER TABLE orders ADD CONSTRAINT ck_orders_total CHECK (total_amount >= 0);
ALTER TABLE inventory_receipt_items ADD CONSTRAINT ck_receipt_items_quantity CHECK (quantity > 0);

-- Index cho các cột khóa ngoại hay truy vấn
CREATE INDEX idx_addresses_user_id             ON addresses (user_id);
CREATE INDEX idx_carts_user_id                 ON carts (user_id);
CREATE INDEX idx_category_type_category_id     ON category_type (category_id);
CREATE INDEX idx_products_category_id          ON products (category_id);
CREATE INDEX idx_products_category_type_id     ON products (category_type_id);
CREATE INDEX idx_product_variant_product_id    ON product_variant (product_id);
CREATE INDEX idx_product_resources_product_id  ON product_resources (product_id);
CREATE INDEX idx_orders_customer_id            ON orders (customer_id);
CREATE INDEX idx_orders_status                 ON orders (status);
CREATE INDEX idx_order_items_order_id          ON order_items (order_id);
CREATE INDEX idx_order_items_product_id        ON order_items (product_id);
CREATE INDEX idx_order_status_history_order_id ON order_status_history (order_id);
CREATE INDEX idx_reviews_product_id            ON reviews (product_id);
CREATE INDEX idx_reviews_user_id               ON reviews (user_id);
CREATE INDEX idx_receipt_items_receipt_id      ON inventory_receipt_items (receipt_id);
CREATE INDEX idx_stock_movements_variant_id    ON stock_movements (product_variant_id);
