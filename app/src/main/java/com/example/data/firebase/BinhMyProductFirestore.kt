package com.example.data.firebase

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Cấu trúc dữ liệu Firestore cho nông sản từ các nông hộ xã Bình Mỹ.
 * Bao gồm các trường theo yêu cầu:
 * - ten_san_pham (Tên sản phẩm)
 * - ten_nong_ho (Tên nông hộ)
 * - gia_du_kien (Giá dự kiến)
 * - ngay_thu_hoach_du_kien (Ngày thu hoạch dự kiến)
 * Kèm thông tin mở rộng: mô tả chi tiết & thông tin liên hệ nhà vườn.
 */
@IgnoreExtraProperties
data class BinhMyProductFirestore(
    @DocumentId
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("ten_san_pham") @set:PropertyName("ten_san_pham")
    var tenSanPham: String = "",

    @get:PropertyName("ten_nong_ho") @set:PropertyName("ten_nong_ho")
    var tenNongHo: String = "",

    @get:PropertyName("gia_du_kien") @set:PropertyName("gia_du_kien")
    var giaDuKien: Double = 0.0,

    @get:PropertyName("ngay_thu_hoach_du_kien") @set:PropertyName("ngay_thu_hoach_du_kien")
    var ngayThuHoachDuKien: String = "",

    // Các trường bổ sung chuẩn hóa phục vụ thương mại điện tử nông sản Bình Mỹ
    @get:PropertyName("don_vi_tinh") @set:PropertyName("don_vi_tinh")
    var donViTinh: String = "kg",

    @get:PropertyName("san_luong_du_kien_kg") @set:PropertyName("san_luong_du_kien_kg")
    var sanLuongDuKienKg: Double = 0.0,

    @get:PropertyName("tieu_chuan_chat_luong") @set:PropertyName("tieu_chuan_chat_luong")
    var tieuChuanChatLuong: String = "VietGAP",

    @get:PropertyName("dia_chi_ap") @set:PropertyName("dia_chi_ap")
    var diaChiAp: String = "Ấp 4A, Xã Bình Mỹ, Huyện Củ Chi, TP.HCM",

    @get:PropertyName("mo_ta") @set:PropertyName("mo_ta")
    var moTa: String = "",

    @get:PropertyName("ma_lo_kiem_dinh") @set:PropertyName("ma_lo_kiem_dinh")
    var maLoKiemDinh: String = "",

    // Chi tiết bổ sung khi mở rộng xem chi tiết (Expandable Detail View)
    @get:PropertyName("so_dien_thoai_lien_he") @set:PropertyName("so_dien_thoai_lien_he")
    var soDienThoaiLienHe: String = "",

    @get:PropertyName("nguoi_dai_dien") @set:PropertyName("nguoi_dai_dien")
    var nguoiDaiDien: String = "",

    @get:PropertyName("dien_tich_canh_tac_ha") @set:PropertyName("dien_tich_canh_tac_ha")
    var dienTichCanhTacHa: Double = 0.0,

    @get:PropertyName("phuong_thuc_canh_tac") @set:PropertyName("phuong_thuc_canh_tac")
    var phuongThucCanhTac: String = "",

    @get:PropertyName("huong_dan_bao_quan") @set:PropertyName("huong_dan_bao_quan")
    var huongDanBaoQuan: String = "",

    @get:PropertyName("created_at") @set:PropertyName("created_at")
    @ServerTimestamp
    var createdAt: Date? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "ten_san_pham" to tenSanPham,
        "ten_nong_ho" to tenNongHo,
        "gia_du_kien" to giaDuKien,
        "ngay_thu_hoach_du_kien" to ngayThuHoachDuKien,
        "don_vi_tinh" to donViTinh,
        "san_luong_du_kien_kg" to sanLuongDuKienKg,
        "tieu_chuan_chat_luong" to tieuChuanChatLuong,
        "dia_chi_ap" to diaChiAp,
        "mo_ta" to moTa,
        "ma_lo_kiem_dinh" to maLoKiemDinh,
        "so_dien_thoai_lien_he" to soDienThoaiLienHe,
        "nguoi_dai_dien" to nguoiDaiDien,
        "dien_tich_canh_tac_ha" to dienTichCanhTacHa,
        "phuong_thuc_canh_tac" to phuongThucCanhTac,
        "huong_dan_bao_quan" to huongDanBaoQuan,
        "updated_at_epoch_ms" to System.currentTimeMillis()
    )
}
