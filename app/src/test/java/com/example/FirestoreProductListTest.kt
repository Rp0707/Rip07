package com.example

import com.example.data.firebase.BinhMyProductFirestore
import com.example.data.firebase.FirebaseService
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FirestoreProductListTest {

    @Test
    fun testBinhMyProductFirestore_fieldsCorrectlyDefined() {
        val product = BinhMyProductFirestore(
            id = "prod_123",
            tenSanPham = "Rau muống nước Bình Mỹ VietGAP",
            tenNongHo = "Hộ Chú Năm - Ấp 4A",
            giaDuKien = 18000.0,
            ngayThuHoachDuKien = "03/10/2026 (04:30 Sáng Thứ Bảy)",
            donViTinh = "kg",
            sanLuongDuKienKg = 250.0,
            tieuChuanChatLuong = "VietGAP"
        )

        assertEquals("Rau muống nước Bình Mỹ VietGAP", product.tenSanPham)
        assertEquals("Hộ Chú Năm - Ấp 4A", product.tenNongHo)
        assertEquals(18000.0, product.giaDuKien, 0.001)
        assertEquals("03/10/2026 (04:30 Sáng Thứ Bảy)", product.ngayThuHoachDuKien)

        val map = product.toMap()
        assertEquals("Rau muống nước Bình Mỹ VietGAP", map["ten_san_pham"])
        assertEquals("Hộ Chú Năm - Ấp 4A", map["ten_nong_ho"])
        assertEquals(18000.0, map["gia_du_kien"])
        assertEquals("03/10/2026 (04:30 Sáng Thứ Bảy)", map["ngay_thu_hoach_du_kien"])
    }

    @Test
    fun testDefaultBinhMyProducts_allHaveRequiredFields() {
        val context = RuntimeEnvironment.getApplication()
        val service = FirebaseService(context)
        val defaultProducts = service.getDefaultBinhMyProducts()

        assertTrue("Danh sách nông sản Bình Mỹ mẫu không được rỗng", defaultProducts.isNotEmpty())

        for (prod in defaultProducts) {
            assertTrue("Tên sản phẩm không được rỗng", prod.tenSanPham.isNotBlank())
            assertTrue("Tên nông hộ không được rỗng", prod.tenNongHo.isNotBlank())
            assertTrue("Giá dự kiến phải lớn hơn 0", prod.giaDuKien > 0)
            assertTrue("Ngày thu hoạch dự kiến không được rỗng", prod.ngayThuHoachDuKien.isNotBlank())
        }
    }
}
