-- P1: thanh toán callback idempotent + thống kê
ALTER TABLE payments ADD COLUMN IF NOT EXISTS payment_ref VARCHAR(100);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
CREATE UNIQUE INDEX IF NOT EXISTS uq_payments_ref ON payments(payment_ref) WHERE payment_ref IS NOT NULL;

-- Index hỗ trợ tìm kiếm nâng cao theo rating
CREATE INDEX IF NOT EXISTS idx_restaurants_status_rating ON restaurants(status, avg_rating DESC, restaurant_id DESC);
