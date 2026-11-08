INSERT INTO categories (name) VALUES
    ('Cơm'), ('Phở & Bún'), ('Bánh mì'), ('Đồ ăn nhanh'),
    ('Đồ uống'), ('Tráng miệng'), ('Đồ chay'), ('Khác')
ON CONFLICT (name) DO NOTHING;

-- Tài khoản admin kiểm thử: admin@fooddelivery.com / Admin@123
-- Hãy đổi mật khẩu ngay sau khi deploy lần đầu.
INSERT INTO users (email, password_hash, full_name, role, is_active)
VALUES ('admin@fooddelivery.com',
        '$2a$10$THlFZoVAuqdpqU.zhDiFxOFap9kHcpqQNkcZ.Y./psfyXUxTd1YiO',
        'Food Delivery Admin', 'ADMIN', TRUE)
ON CONFLICT (email) DO NOTHING;
