IF NOT EXISTS (SELECT 1 FROM categories WHERE name = N'Cơm')
    INSERT INTO categories (name) VALUES (N'Cơm');
IF NOT EXISTS (SELECT 1 FROM categories WHERE name = N'Phở & Bún')
    INSERT INTO categories (name) VALUES (N'Phở & Bún');
IF NOT EXISTS (SELECT 1 FROM categories WHERE name = N'Bánh mì')
    INSERT INTO categories (name) VALUES (N'Bánh mì');
IF NOT EXISTS (SELECT 1 FROM categories WHERE name = N'Đồ ăn nhanh')
    INSERT INTO categories (name) VALUES (N'Đồ ăn nhanh');
IF NOT EXISTS (SELECT 1 FROM categories WHERE name = N'Đồ uống')
    INSERT INTO categories (name) VALUES (N'Đồ uống');
IF NOT EXISTS (SELECT 1 FROM categories WHERE name = N'Tráng miệng')
    INSERT INTO categories (name) VALUES (N'Tráng miệng');
IF NOT EXISTS (SELECT 1 FROM categories WHERE name = N'Đồ chay')
    INSERT INTO categories (name) VALUES (N'Đồ chay');
IF NOT EXISTS (SELECT 1 FROM categories WHERE name = N'Khác')
    INSERT INTO categories (name) VALUES (N'Khác');

IF NOT EXISTS (SELECT 1 FROM users WHERE email = N'admin@fooddelivery.com')
    INSERT INTO users (email, password_hash, full_name, role, is_active)
    VALUES (N'admin@fooddelivery.com',
            N'$2a$10$THlFZoVAuqdpqU.zhDiFxOFap9kHcpqQNkcZ.Y./psfyXUxTd1YiO',
            N'Food Delivery Admin', N'ADMIN', 1);
