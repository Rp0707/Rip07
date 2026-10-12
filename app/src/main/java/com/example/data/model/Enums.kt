package com.example.data.model

enum class UserRole(val labelVi: String) {
    BUYER("Khách hàng"),
    FARMER("Nông hộ"),
    ADMIN("Quản trị viên"),
    POPUP_STAFF("Nhân viên Pop-up"),
    VERIFIER("Xác minh viên"),
    SUPER_ADMIN("Siêu quản trị")
}

enum class SessionStatus(val labelVi: String) {
    OPEN("Đang mở đặt trước"),
    LOCKED("Đã khóa đơn - Chuẩn bị thu hoạch"),
    HARVESTING("Đang thu hoạch & sơ chế"),
    DISTRIBUTING("Đang chuyển đến Pop-up / Giao hàng"),
    COMPLETED("Đã hoàn tất phiên"),
    CANCELLED("Phiên đã hủy")
}

enum class OrderStatus(val labelVi: String) {
    AWAITING_CONFIRMATION("Chờ xác nhận"),
    PLACED("Đã đặt"),
    LOCKED("Đã khóa"),
    PREPARING("Đang chuẩn bị"),
    READY_FOR_PICKUP_OR_DELIVERING("Đã đến điểm nhận / Đang giao"),
    COMPLETED("Hoàn tất"),
    CANCELLED("Đã hủy"),
    ISSUE_PENDING("Có vấn đề cần xử lý")
}

enum class DeliveryType(val labelVi: String) {
    POPUP_PICKUP("Nhận tại điểm Pop-up"),
    HOME_DELIVERY("Giao hàng tận nhà (Thử nghiệm)")
}

enum class RecurringFrequency(val labelVi: String) {
    WEEKLY("Hằng tuần"),
    BIWEEKLY("2 tuần / lần"),
    MONTHLY("Hằng tháng")
}

enum class RecurringStatus(val labelVi: String) {
    ACTIVE("Đang hoạt động"),
    PAUSED("Đang tạm dừng"),
    SKIPPED_ONCE("Bỏ qua 1 kỳ tiếp theo")
}

enum class VerificationStatus(val labelVi: String) {
    PENDING("Chờ kiểm tra"),
    VERIFIED("Đã xác minh VietGAP"),
    REJECTED("Không đạt yêu cầu")
}

enum class NotificationType(val labelVi: String, val iconEmoji: String) {
    SESSION_OPENED("Phiên đặt mới mở", "🌾"),
    ORDER_CONFIRMED("Đơn hàng đã xác nhận", "✅"),
    ORDER_PREPARING("Đơn hàng đang chuẩn bị", "🌱"),
    ORDER_ARRIVED_POPUP("Đơn đã đến điểm nhận", "📦"),
    SHORTAGE_ALERT("Thông báo thiếu hàng", "⚠️"),
    ADMIN_BROADCAST("Cập nhật từ Quản trị viên", "📢"),
    ORDER_DELIVERED("Đã giao hàng thành công", "🎉")
}

