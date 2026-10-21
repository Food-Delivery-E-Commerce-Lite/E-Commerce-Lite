CREATE TABLE users (
    user_id       BIGINT IDENTITY(1,1) PRIMARY KEY,
    email         NVARCHAR(150) NOT NULL UNIQUE,
    password_hash NVARCHAR(100) NOT NULL,
    full_name     NVARCHAR(100) NOT NULL,
    phone         NVARCHAR(20),
    role          NVARCHAR(20) NOT NULL CHECK (role IN ('CUSTOMER','MERCHANT','ADMIN')),
    is_active     BIT NOT NULL DEFAULT 1,
    created_at    DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);

CREATE TABLE addresses (
    address_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(user_id),
    label      NVARCHAR(50),
    street     NVARCHAR(200) NOT NULL,
    district   NVARCHAR(100),
    city       NVARCHAR(100),
    lat        DECIMAL(9,6),
    lng        DECIMAL(9,6),
    is_default BIT NOT NULL DEFAULT 0
);

CREATE TABLE restaurants (
    restaurant_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    owner_id      BIGINT NOT NULL REFERENCES users(user_id),
    name          NVARCHAR(150) NOT NULL,
    description   NVARCHAR(MAX),
    address       NVARCHAR(250),
    lat           DECIMAL(9,6),
    lng           DECIMAL(9,6),
    open_time     TIME,
    close_time    TIME,
    status        NVARCHAR(20) NOT NULL DEFAULT 'PENDING'
                  CHECK (status IN ('PENDING','APPROVED','REJECTED','LOCKED')),
    avg_rating    DECIMAL(3,2) NOT NULL DEFAULT 0,
    rating_count  INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_restaurants_name ON restaurants(name);

CREATE TABLE categories (
    category_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE menu_items (
    item_id       BIGINT IDENTITY(1,1) PRIMARY KEY,
    restaurant_id BIGINT NOT NULL REFERENCES restaurants(restaurant_id),
    category_id   BIGINT REFERENCES categories(category_id),
    name          NVARCHAR(150) NOT NULL,
    description   NVARCHAR(MAX),
    price         DECIMAL(12,0) NOT NULL CHECK (price > 0),
    image_url     NVARCHAR(300),
    is_available  BIT NOT NULL DEFAULT 1
);
CREATE INDEX idx_menu_items_restaurant ON menu_items(restaurant_id, name);

CREATE TABLE orders (
    order_id      BIGINT IDENTITY(1,1) PRIMARY KEY,
    customer_id   BIGINT NOT NULL REFERENCES users(user_id),
    restaurant_id BIGINT NOT NULL REFERENCES restaurants(restaurant_id),
    address_id    BIGINT NOT NULL REFERENCES addresses(address_id),
    status        NVARCHAR(20) NOT NULL DEFAULT 'PENDING',
    subtotal      DECIMAL(12,0) NOT NULL CHECK (subtotal >= 0),
    delivery_fee  DECIMAL(12,0) NOT NULL CHECK (delivery_fee >= 0),
    total_amount  DECIMAL(12,0) NOT NULL CHECK (total_amount >= 0),
    created_at    DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
CREATE INDEX idx_orders_customer ON orders(customer_id);
CREATE INDEX idx_orders_restaurant_status ON orders(restaurant_id, status);
CREATE INDEX idx_orders_created_at ON orders(created_at);

CREATE TABLE order_items (
    order_item_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id      BIGINT NOT NULL REFERENCES orders(order_id) ON DELETE CASCADE,
    item_id       BIGINT NOT NULL REFERENCES menu_items(item_id),
    item_name     NVARCHAR(150) NOT NULL,
    unit_price    DECIMAL(12,0) NOT NULL,
    quantity      INT NOT NULL CHECK (quantity BETWEEN 1 AND 50)
);

CREATE TABLE payments (
    payment_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id   BIGINT NOT NULL UNIQUE REFERENCES orders(order_id),
    method     NVARCHAR(20) NOT NULL CHECK (method IN ('COD','ONLINE')),
    status     NVARCHAR(20) NOT NULL DEFAULT 'PENDING',
    paid_at    DATETIME2
);

CREATE TABLE reviews (
    review_id     BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id      BIGINT NOT NULL UNIQUE REFERENCES orders(order_id),
    user_id       BIGINT NOT NULL REFERENCES users(user_id),
    restaurant_id BIGINT NOT NULL REFERENCES restaurants(restaurant_id),
    rating        INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment       NVARCHAR(MAX),
    created_at    DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);

CREATE TABLE order_status_history (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id    BIGINT NOT NULL REFERENCES orders(order_id) ON DELETE CASCADE,
    from_status NVARCHAR(20),
    to_status   NVARCHAR(20) NOT NULL,
    changed_by  BIGINT REFERENCES users(user_id),
    changed_at  DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);