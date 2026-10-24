# Food Delivery Web App

Nền tảng đặt đồ ăn trực tuyến (bản Lite) với 3 vai trò: Customer, Merchant, Admin.

**Công nghệ:** Java 17, Spring Boot 3, Spring Security (JWT), PostgreSQL, Flyway, React (Vite), Docker.

## Tính năng (MVP)
- [ ] Đăng ký / đăng nhập, phân quyền JWT
- [ ] Nhà hàng, thực đơn, tìm kiếm + phân trang
- [ ] Giỏ hàng và đặt hàng
- [ ] Vòng đời đơn hàng (state machine) và theo dõi trạng thái
- [ ] Đánh giá nhà hàng
- [ ] Dashboard Merchant / Admin

## Nhà hàng và thực đơn
- Nhà hàng mới do MERCHANT tạo sẽ ở trạng thái `PENDING`; chỉ nhà hàng `APPROVED` xuất hiện trong tìm kiếm công khai.
- MERCHANT chỉ có thể xem/sửa nhà hàng và món ăn thuộc tài khoản của mình.
- ADMIN có thể duyệt, từ chối hoặc khóa nhà hàng.
- Flyway `V2` tạo 8 danh mục món ăn và tài khoản kiểm thử `admin@fooddelivery.com` / `Admin@123` nếu tài khoản chưa tồn tại. Đổi mật khẩu này trước khi deploy.
- API công khai: `GET /api/v1/restaurants?keyword=&page=&size=`, `GET /api/v1/restaurants/{id}`, `GET /api/v1/restaurants/{id}/menu`, `GET /api/v1/categories`.
- API MERCHANT: `POST /api/v1/merchant/restaurants`, `GET`/`PUT /api/v1/merchant/restaurants/{id}`, `POST /api/v1/merchant/restaurants/{id}/menu-items`, `PUT /api/v1/merchant/menu-items/{itemId}`, `PATCH /api/v1/merchant/menu-items/{itemId}/availability`.
- API ADMIN: `GET /api/v1/admin/restaurants?status=PENDING`, `PATCH /api/v1/admin/restaurants/{id}/status`.

## Yêu cầu
JDK 17, Maven 3.9+, Node 20+, Docker.

## Chạy nhanh
```bash
cp .env.example .env              # chỉnh biến môi trường
docker compose up -d db           # chạy PostgreSQL
cd backend && mvn spring-boot:run # API: http://localhost:8080
cd frontend && npm install && npm run dev   # UI: http://localhost:5173
```
Chạy toàn bộ bằng Docker: `docker compose up --build`

- Kiểm tra API: http://localhost:8080/api/v1/health
- Swagger UI: http://localhost:8080/swagger-ui.html

## Chạy test
```bash
cd backend && mvn test
```

## Cấu trúc
```
backend/   Spring Boot API (package-by-feature: auth, user, restaurant, order, ...)
frontend/  React SPA
docs/      Tài liệu, ERD, wireframe
```

## Quy trình Git
Nhánh: `main` (ổn định) <- `develop` <- `feature/*`. Commit theo Conventional Commits (`feat(order): ...`).
