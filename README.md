# User Authorization API

### Mô tả Dự án
API dựa trên Spring Boot để quản lý nhân viên/người dùng với các tính năng bao gồm thao tác hàng loạt, quản lý quyền và kiểm soát truy cập dựa trên vị trí.

### Tính Năng Chính
- Thao tác hàng loạt với người dùng (tạo/cập nhật/xóa)
- Quản lý quyền (dựa trên agent và field)
- Kiểm soát truy cập dựa trên vị trí
- Đồng bộ hóa dữ liệu trạm

### Công Nghệ Sử Dụng
- Java 17
- Spring Boot 3.2.3
- MySQL 8.0
- Maven
- JPA/Hibernate

### Cấu Trúc Dự Án
```
src/main/java/com/  user/demo/
├── controller/       # Các endpoint API
├── dto/              # Các đối tượng chuyển đổi dữ liệu
├── exception/        # Xử lý exception
├── model/            # Các model
├── repository/       # Tầng truy cập dữ liệu
└── service/          # Tầng logic 
```

### Hướng Dẫn Cài Đặt
1. Clone repository
2. Cấu hình cơ sở dữ liệu MySQL
3. Cập nhật `application.properties` với thông tin đăng nhập cơ sở dữ liệu
4. Chạy script SQL trong `src/main/resources/static_tables.sql` để tạo các bảng tĩnh.
5. Build và chạy dự án:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

### Các Endpoint API
- `POST /api/users/bulk` - Thao tác hàng loạt với người dùng
- `POST /api/users/update` - Cập nhật quyền người dùng
- `POST /api/stations/sync` - Đồng bộ hóa dữ liệu trạm

### Kiểm Thử
Dữ liệu mẫu để kiểm thử có sẵn trong `src/main/resources/test_samples/`. Để tử tính năng đồng bộ hóa thì không cần mẫu data vì đã có sẵn giả lập trong hàm fetchStationsData(). Chỉ cần chạy request với đúng endpoint (miễn là có username "hienlt11" trong users vì giả lập chỉ dùng users này)

### Cấu Trúc Cơ Sở Dữ Liệu
Dự án sử dụng các bảng chính sau:
- `users` - Thông tin người dùng
- `location_permission` - Quyền dựa trên vị trí
- `areas` - Thông tin khu vực
- `provinces` - Thông tin tỉnh/thành phố
- `districts` - Thông tin quận/huyện
- `nations` - Thông tin quốc gia
- `main_stations` - Thông tin trạm
- `fields` - Thông tin lĩnh vực
- `agents` - Thông tin đại lý 