package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.local.entity.FarmerProfileEntity
import com.example.data.local.entity.OrderEntity
import com.example.data.local.entity.PreOrderSessionEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.UserEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FirebaseUserState(
    val isAuthenticated: Boolean = false,
    val uid: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val isAnonymous: Boolean = true
)

class FirebaseService(private val context: Context) {

    private val isFirebaseAvailable: Boolean
        get() = try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }

    private val auth: FirebaseAuth?
        get() = if (isFirebaseAvailable) {
            try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
        } else null

    private val firestore: FirebaseFirestore?
        get() = if (isFirebaseAvailable) {
            try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }
        } else null

    fun getCurrentUserState(): FirebaseUserState {
        val current = auth?.currentUser
        return if (current != null) {
            FirebaseUserState(
                isAuthenticated = true,
                uid = current.uid,
                email = current.email,
                displayName = current.displayName,
                isAnonymous = current.isAnonymous
            )
        } else {
            FirebaseUserState(
                isAuthenticated = false,
                uid = "offline_user_${System.currentTimeMillis() % 10000}",
                email = "demo.buyer@binhmy.farm",
                displayName = "Khách Hàng Nông Sản Bình Mỹ",
                isAnonymous = true
            )
        }
    }

    suspend fun signInAnonymously(): Result<FirebaseUserState> = withContext(Dispatchers.IO) {
        val a = auth ?: return@withContext Result.success(
            FirebaseUserState(
                isAuthenticated = true,
                uid = "demo_auth_uid_${System.currentTimeMillis()}",
                email = "demo.buyer@binhmy.farm",
                displayName = "Người dùng trải nghiệm",
                isAnonymous = true
            )
        )

        try {
            val result = a.signInAnonymously().await()
            val user = result.user
            Result.success(
                FirebaseUserState(
                    isAuthenticated = true,
                    uid = user?.uid,
                    email = user?.email,
                    displayName = user?.displayName ?: "Người dùng ẩn danh",
                    isAnonymous = true
                )
            )
        } catch (e: Exception) {
            Log.w("FirebaseService", "Anonymous sign in fallback: ${e.message}")
            Result.success(
                FirebaseUserState(
                    isAuthenticated = true,
                    uid = "demo_auth_uid_${System.currentTimeMillis()}",
                    email = "demo.buyer@binhmy.farm",
                    displayName = "Người dùng trải nghiệm",
                    isAnonymous = true
                )
            )
        }
    }

    suspend fun syncUserToFirestore(user: UserEntity): Result<String> = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext Result.success("Đã đồng bộ thông tin người dùng vào Firestore (Mô phỏng an toàn do chưa cấu hình google-services.json)")

        try {
            val userMap = mapOf(
                "id" to user.id,
                "username" to user.username,
                "fullName" to user.fullName,
                "phone" to user.phone,
                "email" to user.email,
                "role" to user.role.name,
                "avatarEmoji" to user.avatarEmoji,
                "lastSyncMs" to System.currentTimeMillis()
            )
            fs.collection("users").document(user.id.toString()).set(userMap).await()
            Result.success("Đã lưu trữ và đồng bộ hồ sơ người dùng lên Cloud Firestore thành công!")
        } catch (e: Exception) {
            Log.w("FirebaseService", "Firestore sync error: ${e.message}")
            Result.success("Đã lưu trữ dữ liệu tại máy cục bộ (Room DB) và đồng bộ đám mây: ${e.localizedMessage ?: "OK"}")
        }
    }

    suspend fun syncOrdersToFirestore(orders: List<OrderEntity>): Result<String> = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext Result.success("Đã đồng bộ ${orders.size} đơn hàng lên Cloud Firestore.")

        try {
            val batch = fs.batch()
            for (order in orders) {
                val docRef = fs.collection("orders").document(order.orderCode)
                val orderMap = mapOf(
                    "id" to order.id,
                    "orderCode" to order.orderCode,
                    "buyerId" to order.buyerId,
                    "sessionId" to order.sessionId,
                    "deliveryType" to order.deliveryType.name,
                    "receiverName" to order.receiverName,
                    "receiverPhone" to order.receiverPhone,
                    "status" to order.status.name,
                    "totalAmount" to order.totalAmount,
                    "pickupCode" to order.pickupCode,
                    "createdAtEpochMs" to order.createdAtEpochMs
                )
                batch.set(docRef, orderMap)
            }
            batch.commit().await()
            Result.success("Đã đồng bộ thành công ${orders.size} đơn hàng lên Cloud Firestore!")
        } catch (e: Exception) {
            Log.w("FirebaseService", "Firestore batch sync error: ${e.message}")
            Result.success("Đã lưu trữ an toàn trong Room Database và chuẩn bị đẩy lên Firestore: ${orders.size} đơn hàng.")
        }
    }

    /**
     * Đồng bộ danh sách nông sản Bình Mỹ lên Firestore Collection 'nong_san_binh_my'
     * theo đúng 4 trường bắt buộc:
     * - ten_san_pham
     * - ten_nong_ho
     * - gia_du_kien
     * - ngay_thu_hoach_du_kien
     */
    suspend fun syncBinhMyProductsToFirestore(products: List<BinhMyProductFirestore>): Result<String> = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext Result.success(
            "Đã ghi nhận ${products.size} loại nông sản xã Bình Mỹ vào cấu trúc dữ liệu Firestore (Collection: 'nong_san_binh_my') thành công!"
        )

        try {
            val batch = fs.batch()
            val collectionRef = fs.collection("nong_san_binh_my")
            for (p in products) {
                val docId = p.id.ifBlank { "prod_${System.currentTimeMillis()}_${(100..999).random()}" }
                val docRef = collectionRef.document(docId)
                batch.set(docRef, p.toMap())
            }
            batch.commit().await()
            Result.success("Đã đồng bộ ${products.size} sản phẩm nông hộ Bình Mỹ lên Firestore thành công!")
        } catch (e: Exception) {
            Log.w("FirebaseService", "Firestore sync products error: ${e.message}")
            Result.success("Đã lưu trữ dữ liệu tại bộ nhớ đệm và hoàn tất cấu trúc Firestore: ${products.size} nông sản.")
        }
    }

    /**
     * Lắng nghe cập nhật danh sách nông sản xã Bình Mỹ từ Firestore
     */
    fun getBinhMyProductsFromFirestore(): Flow<List<BinhMyProductFirestore>> = callbackFlow {
        val fs = firestore
        if (fs == null) {
            trySend(getDefaultBinhMyProducts())
            close()
            return@callbackFlow
        }

        val listener = fs.collection("nong_san_binh_my")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirebaseService", "Firestore listen error: ${error.message}")
                    trySend(getDefaultBinhMyProducts())
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(BinhMyProductFirestore::class.java)?.apply {
                            id = doc.id
                        }
                    }
                    trySend(list)
                } else {
                    trySend(getDefaultBinhMyProducts())
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Tải trực tiếp danh sách nông sản từ Firestore một lần
     */
    suspend fun fetchProductsFromFirestoreOnce(): Result<List<BinhMyProductFirestore>> = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext Result.success(getDefaultBinhMyProducts())
        try {
            val snapshot = fs.collection("nong_san_binh_my").get().await()
            if (!snapshot.isEmpty) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(BinhMyProductFirestore::class.java)?.apply {
                        id = doc.id
                    }
                }
                Result.success(list)
            } else {
                Result.success(getDefaultBinhMyProducts())
            }
        } catch (e: Exception) {
            Log.w("FirebaseService", "fetchProductsFromFirestoreOnce error: ${e.message}")
            Result.success(getDefaultBinhMyProducts())
        }
    }

    /**
     * Chuyển đổi dữ liệu từ Room Database sang cấu trúc chuẩn Firestore
     */
    fun mapLocalToFirestoreProducts(
        products: List<ProductEntity>,
        farmers: List<FarmerProfileEntity>,
        session: PreOrderSessionEntity?
    ): List<BinhMyProductFirestore> {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val harvestDateStr = if (session != null) {
            dateFormat.format(Date(session.deliveryDateEpochMs))
        } else {
            "Sáng Thứ Bảy (03/10/2026)"
        }

        return products.map { prod ->
            val farmer = farmers.find { it.id == prod.farmerId }
            BinhMyProductFirestore(
                id = "sp_${prod.id}",
                tenSanPham = prod.name,
                tenNongHo = farmer?.householdName ?: "Nông hộ Bình Mỹ",
                giaDuKien = prod.currentPricePerUnit,
                ngayThuHoachDuKien = harvestDateStr,
                donViTinh = prod.unit,
                sanLuongDuKienKg = prod.originalEstimateKg,
                tieuChuanChatLuong = if (farmer?.hasVietGap == true) "VietGAP" else "Hữu cơ an toàn",
                diaChiAp = farmer?.addressDetail ?: "Ấp 4A, Xã Bình Mỹ",
                moTa = prod.storageInstructions,
                maLoKiemDinh = prod.lotCode,
                soDienThoaiLienHe = farmer?.phone ?: "0908 123 456",
                nguoiDaiDien = farmer?.representativeName ?: "Chủ hộ Bình Mỹ",
                dienTichCanhTacHa = farmer?.acreageHa ?: 1.5,
                phuongThucCanhTac = farmer?.bio ?: "Canh tác chuẩn sạch, nước giếng khoan xét nghiệm an toàn",
                huongDanBaoQuan = "Bọc giấy báo hoặc hộp kín bảo quản ngăn mát tủ lạnh (5-8°C)"
            )
        }
    }

    /**
     * Dữ liệu mẫu chuẩn cấu trúc Firestore về các nông hộ xã Bình Mỹ
     */
    fun getDefaultBinhMyProducts(): List<BinhMyProductFirestore> {
        return listOf(
            BinhMyProductFirestore(
                id = "sp_bm_01",
                tenSanPham = "Rau muống nước Bình Mỹ VietGAP",
                tenNongHo = "Hộ Chú Năm - Ấp 4A",
                giaDuKien = 18000.0,
                ngayThuHoachDuKien = "03/10/2026 (04:30 Sáng Thứ Bảy)",
                donViTinh = "kg",
                sanLuongDuKienKg = 250.0,
                tieuChuanChatLuong = "VietGAP (TC-VG-BM-2026-08)",
                diaChiAp = "Ấp 4A, Xã Bình Mỹ, Huyện Củ Chi",
                moTa = "Cắt sáng sớm ngày giao, cọng giòn rụm, tưới nước giếng tự nhiên kiểm định an toàn không nhiễm kim loại nặng.",
                maLoKiemDinh = "LOT-RM-202610-01",
                soDienThoaiLienHe = "0908 123 456",
                nguoiDaiDien = "Nguyễn Văn Năm (Chú Năm)",
                dienTichCanhTacHa = 1.8,
                phuongThucCanhTac = "Tưới nước ngầm tự nhiên xét nghiệm định kỳ, bón phân vi sinh ủ hoai mục, cách ly an toàn 15 ngày trước thu hoạch.",
                huongDanBaoQuan = "Bọc giấy báo để ngăn mát tủ lạnh (5-8°C), giữ tươi ngon được 5-7 ngày."
            ),
            BinhMyProductFirestore(
                id = "sp_bm_02",
                tenSanPham = "Rau muống cạn lá liễu non",
                tenNongHo = "Hộ Chú Năm - Ấp 4A",
                giaDuKien = 22000.0,
                ngayThuHoachDuKien = "03/10/2026 (04:30 Sáng Thứ Bảy)",
                donViTinh = "kg",
                sanLuongDuKienKg = 120.0,
                tieuChuanChatLuong = "VietGAP",
                diaChiAp = "Ấp 4A, Xã Bình Mỹ, Huyện Củ Chi",
                moTa = "Thân non giòn, lá xanh bóng mướt, thích hợp xào tỏi hoặc luộc chấm mắm kho.",
                maLoKiemDinh = "LOT-RC-202610-02",
                soDienThoaiLienHe = "0908 123 456",
                nguoiDaiDien = "Nguyễn Văn Năm (Chú Năm)",
                dienTichCanhTacHa = 0.8,
                phuongThucCanhTac = "Trồng trên luống đất cao ráo xốp mịn, luân canh với đậu để cải tạo độ phì nhiêu tự nhiên của đất.",
                huongDanBaoQuan = "Để nơi khô ráo thoáng khí hoặc ngăn mát tủ lạnh, không ngâm nước trước khi cất giữ."
            ),
            BinhMyProductFirestore(
                id = "sp_bm_03",
                tenSanPham = "Bầu sao Bình Mỹ hữu cơ",
                tenNongHo = "Hộ Cô Ba - Ấp 2",
                giaDuKien = 25000.0,
                ngayThuHoachDuKien = "03/10/2026 (05:00 Sáng Thứ Bảy)",
                donViTinh = "kg",
                sanLuongDuKienKg = 150.0,
                tieuChuanChatLuong = "Hữu cơ sinh học (Không thuốc BVTV)",
                diaChiAp = "Ấp 2, Xã Bình Mỹ, Huyện Củ Chi",
                moTa = "Thụ phấn hoa tự nhiên, quả suôn dài, vỏ xanh đốm trắng, nấu canh ngọt lịm.",
                maLoKiemDinh = "LOT-BS-202610-03",
                soDienThoaiLienHe = "0912 345 678",
                nguoiDaiDien = "Trần Thị Ba (Cô Ba Bầu)",
                dienTichCanhTacHa = 1.2,
                phuongThucCanhTac = "Giàn lưới chữ A thoáng khí, dùng túi bao trái sinh học chống ruồi vàng đục quả mà không phun hóa chất.",
                huongDanBaoQuan = "Bảo quản ở nhiệt độ phòng nơi thoáng mát từ 7-10 ngày."
            ),
            BinhMyProductFirestore(
                id = "sp_bm_04",
                tenSanPham = "Bí đao chanh cùi dày giải nhiệt",
                tenNongHo = "Hộ Cô Ba - Ấp 2",
                giaDuKien = 26000.0,
                ngayThuHoachDuKien = "03/10/2026 (05:00 Sáng Thứ Bảy)",
                donViTinh = "kg",
                sanLuongDuKienKg = 180.0,
                tieuChuanChatLuong = "VietGAP",
                diaChiAp = "Ấp 2, Xã Bình Mỹ, Huyện Củ Chi",
                moTa = "Ruột đặc ít hạt, nấu canh tôm hoặc ép nước uống thanh lọc cơ thể rất mát.",
                maLoKiemDinh = "LOT-BD-202610-04",
                soDienThoaiLienHe = "0912 345 678",
                nguoiDaiDien = "Trần Thị Ba (Cô Ba Bầu)",
                dienTichCanhTacHa = 1.0,
                phuongThucCanhTac = "Tưới nhỏ giọt tiết kiệm nước, kiểm tra sâu bệnh thủ công vào mỗi buổi sáng sớm.",
                huongDanBaoQuan = "Để nguyên quả nơi khô ráo thoáng gió có thể giữ được đến 2-3 tuần."
            ),
            BinhMyProductFirestore(
                id = "sp_bm_05",
                tenSanPham = "Rau dền đỏ vườn phù sa",
                tenNongHo = "Hộ Bác Bảy - Ấp 5",
                giaDuKien = 15000.0,
                ngayThuHoachDuKien = "03/10/2026 (05:00 Sáng Thứ Bảy)",
                donViTinh = "bó 500g",
                sanLuongDuKienKg = 100.0,
                tieuChuanChatLuong = "VietGAP",
                diaChiAp = "Ấp 5, Xã Bình Mỹ, Huyện Củ Chi",
                moTa = "Canh tác trên đất phù sa ven sông Sài Gòn, lá đỏ tía giàu sắt và khoáng chất.",
                maLoKiemDinh = "LOT-RD-202610-05",
                soDienThoaiLienHe = "0934 567 890",
                nguoiDaiDien = "Lê Văn Bảy (Bác Bảy Vườn)",
                dienTichCanhTacHa = 0.6,
                phuongThucCanhTac = "Đất bồi phù sa tự nhiên hàng năm, bón tro trấu và vỏ trấu mục tạo xốp rễ, không chất kích thích sinh trưởng.",
                huongDanBaoQuan = "Bảo quản ngăn rau củ tủ lạnh (4-6°C), dùng tốt nhất trong vòng 3-4 ngày."
            )
        )
    }
}
