# Wings Up (Edu Success) – Hệ thống quản lý kho

Spring Boot 3 (Java 17) + Vue 3 (Vite). CSDL mặc định H2 (tự tạo), có thể đổi sang MySQL trong `backend/src/main/resources/application.properties`.

## Chạy
```bash
# Backend (cổng 8080) – mở thư mục backend bằng IntelliJ rồi chạy WarehouseApplication, hoặc:
cd backend && mvn spring-boot:run

# Frontend (cổng 5173)
cd frontend && npm install && npm run dev
```
Mở http://localhost:5173

Tài khoản mẫu: `admin / admin123` (ADMIN) · `staff / staff123` (USER)

## Tổng hợp chức năng

| Chức năng | USER (nhân viên) | ADMIN |
|---|---|---|
| Đăng nhập / đăng xuất / đổi mật khẩu | ✔ | ✔ |
| Xem tổng quan (số SP, tồn, giá trị, sắp hết hàng) | ✔ | ✔ |
| Xem & tìm kiếm sản phẩm, xem tồn kho | ✔ | ✔ |
| Thêm / sửa / xóa sản phẩm | ✘ | ✔ |
| Quản lý danh mục | ✘ | ✔ |
| Quản lý nhà cung cấp | ✘ | ✔ |
| Tạo phiếu nhập/xuất | ✔ (chờ duyệt) | ✔ (tự duyệt, cập nhật tồn ngay) |
| Xem lịch sử phiếu | chỉ phiếu của mình | tất cả |
| Hủy phiếu đang chờ | phiếu của mình | mọi phiếu |
| Duyệt / từ chối phiếu | ✘ | ✔ |
| Quản lý người dùng (thêm, sửa, khóa, đổi quyền, đặt lại mật khẩu) | ✘ | ✔ |

Quy tắc: tồn kho chỉ thay đổi qua phiếu; phiếu xuất không được vượt quá tồn.
Phân quyền được chặn ở backend (`AuthInterceptor`), giao diện chỉ ẩn/hiện menu.
