CREATE TABLE users (
    user_id        BIGSERIAL PRIMARY KEY,
    email          VARCHAR(150) NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,
    full_name      VARCHAR(100) NOT NULL,
    phone          VARCHAR(20),
    role           VARCHAR(20)  NOT NULL CHECK (role IN ('CUSTOMER','MERCHANT','ADMIN')),
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE addresses (
    address_id BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(user_id),
    label      VARCHAR(50),
    street     VARCHAR(200) NOT NULL,
    district   VARCHAR(100),
    city       VARCHAR(100),
    lat        DECIMAL(9,6),
    lng        DECIMAL(9,6),
    is_default BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE restaurants (
    restaurant_id BIGSERIAL PRIMARY KEY,
    owner_id      BIGINT NOT NULL REFERENCES users(user_id),
    name          VARCHAR(150) NOT NULL,
    description   TEXT,
    address       VARCHAR(250),
    lat           DECIMAL(9,6),
    lng           DECIMAL(9,6),
    open_time     TIME,
    close_time    TIME,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                  CHECK (status IN ('PENDING','APPROVED','REJECTED','LOCKED')),
    avg_rating    DECIMAL(3,2) NOT NULL DEFAULT 0,
    rating_count  INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_restaurants_name ON restaurants(name);

CREATE TABLE categories (
    category_id BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE menu_items (
    item_id       BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL REFERENCES restaurants(restaurant_id),
    category_id   BIGINT REFERENCES categories(category_id),
    name          VARCHAR(150) NOT NULL,
    description   TEXT,
    price         NUMERIC(12,0) NOT NULL CHECK (price > 0),
    image_url     VARCHAR(300),
    is_available  BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_menu_items_restaurant ON menu_items(restaurant_id, name);

CREATE TABLE orders (
    order_id      BIGSERIAL PRIMARY KEY,
    customer_id   BIGINT NOT NULL REFERENCES users(user_id),
    restaurant_id BIGINT NOT NULL REFERENCES restaurants(restaurant_id),
    address_id    BIGINT NOT NULL REFERENCES addresses(address_id),
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    subtotal      NUMERIC(12,0) NOT NULL CHECK (subtotal >= 0),
    delivery_fee  NUMERIC(12,0) NOT NULL CHECK (delivery_fee >= 0),
    total_amount  NUMERIC(12,0) NOT NULL CHECK (total_amount >= 0),
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_orders_customer ON orders(customer_id);
CREATE INDEX idx_orders_restaurant_status ON orders(restaurant_id, status);
CREATE INDEX idx_orders_created_at ON orders(created_at);

CREATE TABLE order_items (
    order_item_id BIGSERIAL PRIMARY KEY,
    order_id      BIGINT NOT NULL REFERENCES orders(order_id) ON DELETE CASCADE,
    item_id       BIGINT NOT NULL REFERENCES menu_items(item_id),
    item_name     VARCHAR(150) NOT NULL,
    unit_price    NUMERIC(12,0) NOT NULL,
    quantity      INT NOT NULL CHECK (quantity BETWEEN 1 AND 50)
);

CREATE TABLE payments (
    payment_id BIGSERIAL PRIMARY KEY,
    order_id   BIGINT NOT NULL UNIQUE REFERENCES orders(order_id),
    method     VARCHAR(20) NOT NULL CHECK (method IN ('COD','ONLINE')),
    status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    paid_at    TIMESTAMP
);

CREATE TABLE reviews (
    review_id     BIGSERIAL PRIMARY KEY,
    order_id      BIGINT NOT NULL UNIQUE REFERENCES orders(order_id),
    user_id       BIGINT NOT NULL REFERENCES users(user_id),
    restaurant_id BIGINT NOT NULL REFERENCES restaurants(restaurant_id),
    rating        INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment       TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE order_status_history (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT NOT NULL REFERENCES orders(order_id) ON DELETE CASCADE,
    from_status VARCHAR(20),
    to_status   VARCHAR(20) NOT NULL,
    changed_by  BIGINT REFERENCES users(user_id),
    changed_at  TIMESTAMP NOT NULL DEFAULT now()
);
