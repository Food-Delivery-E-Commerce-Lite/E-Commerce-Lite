-- P2-2: upload ảnh thật (cover quán + tăng độ dài URL món)
ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);
ALTER TABLE menu_items ALTER COLUMN image_url TYPE VARCHAR(500);
