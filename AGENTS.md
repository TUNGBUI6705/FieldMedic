# FieldMedic - Quy tắc cho AI

## Dự án
App Android offline-first cho nhân viên y tế thôn bản. Kotlin + Jetpack Compose + Material 3.

## Kiến trúc (KHÔNG tự ý thay đổi)
- MVVM + Clean Architecture: package data / domain / ui
- DI: Hilt. Điều hướng: Navigation Compose. Bất đồng bộ: Coroutines + Flow
- Room + SQLCipher là nguồn dữ liệu duy nhất. UI chỉ đọc từ Room
- Firestore chỉ là server đồng bộ, tắt persistence của SDK
- Use case trả về Result với sealed class AppError

## Quy tắc code
- Mỗi lần chỉ làm MỘT tính năng tôi yêu cầu, không làm lan sang phần khác
- Không thêm thư viện mới nếu chưa hỏi tôi
- Mọi chuỗi hiển thị để trong strings.xml (tiếng Việt)
- Không bao giờ log dữ liệu bệnh nhân (Timber, Crashlytics)
- ID dùng UUID tạo ở client, xóa mềm bằng is_deleted
- Sau khi sửa phải đảm bảo project build được (./gradlew assembleDebug)
- Viết unit test cho logic thuần (parser, merge, validation)
- Mọi dependency khai báo trong gradle/libs.versions.toml, dùng libs.xxx trong build.gradle.kts.
       Không hardcode phiên bản. Không thêm thư viện mới nếu chưa hỏi tôi.
    - minSdk = 30 (Android 11). Không viết nhánh tương thích cho API < 30.
    - Package gốc: com.example.fieldmedic
## Khi xong mỗi tác vụ
Tóm tắt: file đã tạo/sửa, cách chạy thử, phần chưa làm.


## Luật riêng cho tầng Room (BẮT BUỘC)

Package gốc: com.example.fieldmedic. minSdk 30. Kotlin. Dùng KSP (không kapt).
Vị trí code: com.example.fieldmedic.data.local.{db, entity, dao, relation, view, crypto, repository}
Tiện ích dùng chung: com.example.fieldmedic.util

Quy ước dữ liệu:
- id: String UUID tạo ở client, KHÔNG autoGenerate.
- Mọi thời gian: Long epoch mili giây UTC. Ngày sinh/ngày: String "yyyy-MM-dd".
- Enum: để Room lưu trực tiếp (TEXT tên enum). KHÔNG viết TypeConverter cho enum.
- JSON: lưu String thô ở cột *_json. KHÔNG viết TypeConverter.
- Mỗi entity một file. Tên bảng/cột đúng spec (snake_case qua @ColumnInfo(name=...)), tên property camelCase.
- Annotation của Room có tham số (ForeignKey, Index, ...) PHẢI dùng tham số có tên, ví dụ:
  ForeignKey(entity = X::class, parentColumns = ["id"], childColumns = ["x_id"], onDelete = ForeignKey.CASCADE)
- Ghi dữ liệu dùng @Upsert. CẤM @Insert(onConflict = OnConflictStrategy.REPLACE) (nó xóa dòng cha và kéo theo xóa bảng con).
- CẤM fallbackToDestructiveMigration và allowMainThreadQueries.
- exportSchema = true, schemaLocation = app/schemas.
- Mọi cột là FK phải có Index.
- Không log dữ liệu bệnh nhân, mật khẩu, khóa. Mật khẩu dùng CharArray, khóa dùng ByteArray, xóa mảng sau khi dùng. KHÔNG dùng String cho mật khẩu/khóa.
- Chưa dùng Hilt trong tầng này: class thuần, constructor rõ ràng, để gắn DI sau.
- Không thêm thư viện hoặc đổi cấu trúc ngoài phạm vi prompt. Không tự bịa phiên bản thư viện: chọn bản stable mới nhất tương thích với Kotlin/AGP của project và nêu rõ bản đã chọn.
- Nếu spec thiếu hoặc mâu thuẫn: DỪNG và hỏi, không tự đoán.
- Cuối mỗi tác vụ: project phải build được; liệt kê file đã tạo/sửa; nêu cách chạy test.


## Quy tắc bổ sung (bắt buộc)
- KHÔNG đặt try/catch, runCatching hoặc wrapper lỗi BÊN TRONG withTransaction.
  Bọc lỗi ở NGOÀI transaction, để lỗi làm transaction rollback.
- KHÔNG gắn @Serializable lên entity Room. Payload đồng bộ dùng lớp DTO riêng + hàm ánh xạ.
- Cột dẫn xuất hoặc chỉ cục bộ (last_encounter_at, alert_level, has_danger_sign,
  sync_status, server_updated_at, version, ...) KHÔNG được vào payload/base_values.
- Mỗi cột mới của entity đồng bộ phải được xếp vào SYNCED hoặc LOCAL_ONLY (test sẽ kiểm tra).
- error_log chỉ lưu mã lỗi + tên lớp exception, không lưu nội dung thông báo.
- Mọi assert test phải cụ thể (kiểu exception, giá trị). Không bao giờ sửa test để làm xanh.
- Khi test đỏ: nêu nguyên nhân gốc kèm bằng chứng trước, rồi mới sửa.
- CREATE trong sync_queue luôn có base_version = 0.
- Đọc cấu hình minSdk/package từ build.gradle.kts, không hard-code trong code.