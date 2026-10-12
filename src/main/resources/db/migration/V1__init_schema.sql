-- V1: schema ban đầu, tương ứng các entity JPA (Hibernate 6, PostgreSQL).
-- DB đã có sẵn schema này (tạo bởi ddl-auto=update) sẽ được baseline ở V1 và bỏ qua file này.

-- ===== Tài khoản =====
CREATE TABLE auth_user_details (
    id                uuid         NOT NULL,
    email             varchar(255) NOT NULL,
    password          varchar(255),
    first_name        varchar(255),
    last_name         varchar(255),
    phone_number      varchar(255),
    avatar            varchar(255),
    provider          varchar(255),
    verification_code varchar(255),
    enabled           boolean      NOT NULL,
    created_on        timestamp(6),
    updated_on        timestamp(6),
    CONSTRAINT pk_auth_user_details PRIMARY KEY (id),
    CONSTRAINT uq_auth_user_details_email UNIQUE (email)
);

CREATE TABLE auth_authority (
    id               uuid         NOT NULL,
    role_code        varchar(255) NOT NULL,
    role_description varchar(255) NOT NULL,
    CONSTRAINT pk_auth_authority PRIMARY KEY (id)
);

CREATE TABLE auth_user_authority (
    user_id        uuid NOT NULL,
    authorities_id uuid NOT NULL,
    CONSTRAINT fk_user_authority_user      FOREIGN KEY (user_id)        REFERENCES auth_user_details (id),
    CONSTRAINT fk_user_authority_authority FOREIGN KEY (authorities_id) REFERENCES auth_authority (id)
);

CREATE TABLE addresses (
    id           uuid         NOT NULL,
    user_id      uuid         NOT NULL,
    name         varchar(255),
    street       varchar(255) NOT NULL,
    city         varchar(255) NOT NULL,
    state        varchar(255) NOT NULL,
    zip_code     varchar(255) NOT NULL,
    phone_number varchar(255) NOT NULL,
    CONSTRAINT pk_addresses PRIMARY KEY (id),
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES auth_user_details (id)
);

-- ===== Catalog =====
CREATE TABLE categories (
    id          uuid         NOT NULL,
    name        varchar(255) NOT NULL,
    code        varchar(255) NOT NULL,
    description varchar(255) NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id)
);

CREATE TABLE category_type (
    id          uuid         NOT NULL,
    category_id uuid         NOT NULL,
    name        varchar(255) NOT NULL,
    code        varchar(255) NOT NULL,
    description varchar(255),
    CONSTRAINT pk_category_type PRIMARY KEY (id),
    CONSTRAINT fk_category_type_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE TABLE products (
    id               uuid          NOT NULL,
    category_id      uuid          NOT NULL,
    category_type_id uuid          NOT NULL,
    name             varchar(255)  NOT NULL,
    description      varchar(255),
    price            numeric(38,2) NOT NULL,
    brand            varchar(255)  NOT NULL,
    rating           float4,
    is_new_arrival   boolean       NOT NULL,
    slug             varchar(255)  NOT NULL,
    created_at       timestamp(6)  NOT NULL,
    updated_at       timestamp(6)  NOT NULL,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uq_products_slug UNIQUE (slug),
    CONSTRAINT fk_products_category      FOREIGN KEY (category_id)      REFERENCES categories (id),
    CONSTRAINT fk_products_category_type FOREIGN KEY (category_type_id) REFERENCES category_type (id)
);

CREATE TABLE product_variant (
    id             uuid         NOT NULL,
    product_id     uuid         NOT NULL,
    color          varchar(255) NOT NULL,
    size           varchar(255) NOT NULL,
    stock_quantity integer      NOT NULL,
    CONSTRAINT pk_product_variant PRIMARY KEY (id),
    CONSTRAINT fk_product_variant_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE product_resources (
    id         uuid         NOT NULL,
    product_id uuid         NOT NULL,
    name       varchar(255) NOT NULL,
    url        varchar(255) NOT NULL,
    type       varchar(255) NOT NULL,
    is_primary boolean      NOT NULL,
    CONSTRAINT pk_product_resources PRIMARY KEY (id),
    CONSTRAINT fk_product_resources_product FOREIGN KEY (product_id) REFERENCES products (id)
);

-- ===== Giỏ hàng, đơn hàng, đánh giá =====
CREATE TABLE carts (
    id                 uuid         NOT NULL,
    user_id            uuid         NOT NULL,
    product_id         uuid         NOT NULL,
    product_variant_id uuid         NOT NULL,
    quantity           integer      NOT NULL,
    created_at         timestamp(6) NOT NULL,
    updated_at         timestamp(6) NOT NULL,
    CONSTRAINT pk_carts PRIMARY KEY (id),
    CONSTRAINT fk_carts_user    FOREIGN KEY (user_id)            REFERENCES auth_user_details (id),
    CONSTRAINT fk_carts_product FOREIGN KEY (product_id)         REFERENCES products (id),
    CONSTRAINT fk_carts_variant FOREIGN KEY (product_variant_id) REFERENCES product_variant (id)
);

CREATE TABLE orders (
    id               uuid          NOT NULL,
    customer_id      uuid          NOT NULL,
    order_number     varchar(255)  NOT NULL,
    order_date       timestamp(6)  NOT NULL,
    total_amount     numeric(10,2) NOT NULL,
    status           varchar(255)  NOT NULL,
    payment_status   varchar(255)  NOT NULL,
    payment_method   varchar(255)  NOT NULL,
    shipping_address varchar(255)  NOT NULL,
    notes            text,
    updated_at       timestamp(6),
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT uq_orders_order_number UNIQUE (order_number),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES auth_user_details (id),
    CONSTRAINT orders_status_check CHECK (status IN ('PENDING','SHIPPING','WAIT_DELIVER','PAID','CANCELED','REFUND')),
    CONSTRAINT orders_payment_status_check CHECK (payment_status IN ('FAILED','UNPAID','PAID','REFUND'))
);

CREATE TABLE order_items (
    id                 uuid          NOT NULL,
    order_id           uuid          NOT NULL,
    product_id         uuid          NOT NULL,
    product_variant_id uuid          NOT NULL,
    quantity           integer       NOT NULL,
    unit_price         numeric(10,2) NOT NULL,
    total_price        numeric(10,2) NOT NULL,
    is_reviewed        boolean,
    CONSTRAINT pk_order_items PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order   FOREIGN KEY (order_id)           REFERENCES orders (id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id)         REFERENCES products (id),
    CONSTRAINT fk_order_items_variant FOREIGN KEY (product_variant_id) REFERENCES product_variant (id)
);

CREATE TABLE order_status_history (
    id         uuid         NOT NULL,
    order_id   uuid         NOT NULL,
    old_status varchar(255) NOT NULL,
    new_status varchar(255) NOT NULL,
    changed_by varchar(255) NOT NULL,
    created_at timestamp(6) NOT NULL,
    CONSTRAINT pk_order_status_history PRIMARY KEY (id),
    CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT order_status_history_old_status_check CHECK (old_status IN ('PENDING','SHIPPING','WAIT_DELIVER','PAID','CANCELED','REFUND')),
    CONSTRAINT order_status_history_new_status_check CHECK (new_status IN ('PENDING','SHIPPING','WAIT_DELIVER','PAID','CANCELED','REFUND'))
);

CREATE TABLE reviews (
    id            uuid    NOT NULL,
    user_id       uuid    NOT NULL,
    product_id    uuid    NOT NULL,
    order_item_id uuid    NOT NULL,
    rating        integer NOT NULL,
    comment       text,
    created_at    timestamp(6),
    updated_at    timestamp(6),
    CONSTRAINT pk_reviews PRIMARY KEY (id),
    CONSTRAINT fk_reviews_user       FOREIGN KEY (user_id)       REFERENCES auth_user_details (id),
    CONSTRAINT fk_reviews_product    FOREIGN KEY (product_id)    REFERENCES products (id),
    CONSTRAINT fk_reviews_order_item FOREIGN KEY (order_item_id) REFERENCES order_items (id)
);

-- ===== Kho =====
CREATE TABLE inventory_receipts (
    id             uuid          NOT NULL,
    receipt_number varchar(255)  NOT NULL,
    supplier_name  varchar(255)  NOT NULL,
    receipt_date   timestamp(6)  NOT NULL,
    total_amount   numeric(10,2) NOT NULL,
    status         varchar(255)  NOT NULL,
    notes          text,
    CONSTRAINT pk_inventory_receipts PRIMARY KEY (id),
    CONSTRAINT uq_inventory_receipts_number UNIQUE (receipt_number)
);

CREATE TABLE inventory_receipt_items (
    id                 uuid          NOT NULL,
    receipt_id         uuid          NOT NULL,
    product_id         uuid          NOT NULL,
    product_variant_id uuid          NOT NULL,
    quantity           integer       NOT NULL,
    unit_cost          numeric(10,2) NOT NULL,
    total_cost         numeric(10,2) NOT NULL,
    CONSTRAINT pk_inventory_receipt_items PRIMARY KEY (id),
    CONSTRAINT fk_receipt_items_receipt FOREIGN KEY (receipt_id)         REFERENCES inventory_receipts (id),
    CONSTRAINT fk_receipt_items_product FOREIGN KEY (product_id)         REFERENCES products (id),
    CONSTRAINT fk_receipt_items_variant FOREIGN KEY (product_variant_id) REFERENCES product_variant (id)
);

CREATE TABLE stock_movements (
    id                 uuid         NOT NULL,
    product_id         uuid         NOT NULL,
    product_variant_id uuid         NOT NULL,
    movement_type      varchar(255) NOT NULL,
    quantity           integer      NOT NULL,
    previous_stock     integer      NOT NULL,
    new_stock          integer      NOT NULL,
    reference_id       uuid         NOT NULL,
    reference_type     varchar(255) NOT NULL,
    movement_date      timestamp(6) NOT NULL,
    notes              text,
    CONSTRAINT pk_stock_movements PRIMARY KEY (id),
    CONSTRAINT fk_stock_movements_product FOREIGN KEY (product_id)         REFERENCES products (id),
    CONSTRAINT fk_stock_movements_variant FOREIGN KEY (product_variant_id) REFERENCES product_variant (id)
);
