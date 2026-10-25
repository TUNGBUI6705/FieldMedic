# 📜 Quy Chuẩn Phát Triển & Đóng Góp Code (Contributing Guidelines)

Tài liệu này quy định các tiêu chuẩn kỹ thuật, quy trình lập trình và nguyên tắc kiểm thử bắt buộc đối với tất cả lập trình viên đóng góp cho dự án **FieldMedic**.

---

## 1. Quy Chuẩn Kiến Trúc & Code Kotlin

- **Ngôn ngữ**: Kotlin (bản mới nhất tương thích với project).
- **Phân tầng kiến trúc**: Mọi tính năng phải tuân thủ phân tầng `ui` $\rightarrow$ `domain` $\rightarrow$ `data`. UI không được phép truy cập trực tiếp vào DAO hay Entity của Room.
- **Dependency Injection**: Bắt buộc dùng Hilt (`@HiltViewModel`, `@Inject`, Hilt Modules).
- **Quản lý Chuỗi Văn Bản**: Tất cả chuỗi hiển thị trên giao diện người dùng phải được khai báo trong `res/values/strings.xml` (Tiếng Việt). Không hardcode chuỗi trong Composable.

---

## 2. Quy Chuẩn Dành Cho Tầng CSDL Room

- **Entity & Table Mapping**:
  - Mỗi Entity một tệp riêng nằm trong `data/local/entity/`.
  - Tên bảng và tên cột dùng dạng `snake_case` qua `@Table(name=...)` và `@ColumnInfo(name=...)`. Tên thuộc tính Kotlin dùng `camelCase`.
  - Annotation có tham số (như `@ForeignKey`, `@Index`) bắt buộc dùng tham số có tên (named parameters).
  - Khóa chính dùng **UUID v4** dạng `String` tạo ở Client, KHÔNG dùng `autoGenerate`.
  - Mọi cột Khóa Ngoại (Foreign Key) bắt buộc phải có `@Index`.
- **Thao tác Ghi Dữ Liệu**:
  - Dùng `@Upsert` thay cho `@Insert(onConflict = REPLACE)` để tránh hành vi xóa dòng cha dẫn đến xáo trộn CASCADE dòng con.
  - CẤM dùng `fallbackToDestructiveMigration()` hoặc `allowMainThreadQueries()`.
  - Không gắn `@Serializable` trực tiếp lên Entity Room. Payload đồng bộ dùng lớp DTO riêng (`WireDTO`).
- **Giao Dịch CSDL (Transactions)**:
  - Bọc giao dịch bằng `withTransaction`.
  - **CẤM** đặt khối `try-catch` BÊN TRONG `withTransaction`. Bọc `try-catch` BÊN NGOÀI để lỗi gây rollback giao dịch tự động.

---

## 3. Quy Chuẩn Giao Diện (UI - Jetpack Compose & Material 3)

- **Cấu trúc Màn hình**:
  - Mỗi màn hình bao gồm: `XxxUiState` (Immutable data class), `XxxUiEvent` (Sealed interface), `XxxViewModel`, `XxxScreen` (Stateful wrapper) và `XxxContent` (Stateless, previewable).
  - Mọi `XxxContent` phải có `@Preview` với dữ liệu giả định (sample data) ở chủ đề sáng (Light theme).
- **Tương tác & Accessibility**:
  - Kích thước vùng bấm (Touch Target) $\ge 48\text{dp}$.
  - Kích thước chữ cơ quan $\ge 16\text{sp}$.
  - Dùng màu trạng thái từ Extended Theme (Thành công/Cảnh báo/Nguy hiểm), luôn kết hợp màu sắc với Icon hoặc Text.

---

## 4. Nguyên Tắc An Toàn Dữ Liệu Y Tế (PII)

- **CẤM** ghi thông tin định danh bệnh nhân (Họ tên, CCCD, thông tin liên lạc, tiền sử bệnh) ra Logcat, Timber hay Crashlytics.
- Mọi mảng khóa mã hóa phải xóa sạch bằng `fill(0)` ngay sau khi sử dụng.

---

## 5. Quy Trình Kiểm Thử & Kiểm Tra Code (Verification Checklist)

Trước khi gửi Pull Request hoặc hoàn tất tác vụ, bạn bắt buộc phải chạy và pass 100% các lệnh kiểm thử sau:

```bash
# 1. Kiểm tra biên dịch thành công
./gradlew assembleDebug

# 2. Chạy toàn bộ Unit Test logic nghiệp vụ (Parser, Merge, Validation)
./gradlew testDebugUnitTest

# 3. Chạy Instrumental Integration Test cho CSDL Room (Nếu có sửa DAO/Database)
./gradlew connectedDebugAndroidTest
```
