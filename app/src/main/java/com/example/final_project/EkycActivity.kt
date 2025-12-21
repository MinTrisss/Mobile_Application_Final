package com.example.final_project

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.app.Activity
import java.io.ByteArrayOutputStream
import com.google.firebase.auth.FirebaseAuth
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class EkycActivity : AppCompatActivity() {

    private lateinit var imgFace: ImageView
    private lateinit var btnCapture: Button
    private lateinit var btnSubmit: Button

    private var faceBitmap: Bitmap? = null
    private val CAMERA_REQUEST_CODE = 1001
    private val TAKE_PHOTO_REQUEST_CODE = 2001 // Request code cho intent camera

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance("gs://final-project-7a8d6.firebasestorage.app")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ekyc)

        imgFace = findViewById(R.id.imgFace)
        btnCapture = findViewById(R.id.btnCapture)
        btnSubmit = findViewById(R.id.btnSubmit)

        btnCapture.setOnClickListener { openCameraWithPermissionCheck() }
        btnSubmit.setOnClickListener { handleEkycLogic() }
    }

    private fun openCameraWithPermissionCheck() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE
            )
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivityForResult(intent, TAKE_PHOTO_REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(this, "Không thể mở Camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TAKE_PHOTO_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as? Bitmap
            imageBitmap?.let {
                faceBitmap = it
                imgFace.setImageBitmap(it)
            }
        }
    }

    private fun submitEkyc() {
        val bitmap = faceBitmap ?: run {
            Toast.makeText(this, "Vui lòng chụp ảnh khuôn mặt", Toast.LENGTH_SHORT).show()
            return
        }

        // Kiểm tra user đã login chưa để tránh crash
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Phiên đăng nhập hết hạn", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = user.uid
        val ref = storage.reference.child("ekyc/customers/$uid/face_${System.currentTimeMillis()}.jpg")

        // Nén ảnh chất lượng cao hơn (90-100)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        // Hiển thị loading (nếu có)
        btnSubmit.isEnabled = false
        btnSubmit.text = "Đang tải lên..."

        ref.putBytes(data)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    updateFirestore(uid, uri.toString())
                }
            }
            .addOnFailureListener {
                btnSubmit.isEnabled = true
                btnSubmit.text = "Xác nhận"
                Toast.makeText(this, "Lỗi tải ảnh: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateFirestore(uid: String, imageUrl: String) {
        val updates = mapOf(
            "faceImageURL" to imageUrl,
            "ekycStatus" to "verified"
        )

        db.collection("customers").document(uid)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Đã lưu thông tin eKYC. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show()

                // Đăng xuất để xóa session cũ
                auth.signOut()

                // Quay lại màn hình Login
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                btnSubmit.isEnabled = true
                Toast.makeText(this, "Lỗi cập nhật dữ liệu", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleEkycLogic() {
        val isLoginMode = intent.getBooleanExtra("IS_LOGIN_MODE", false)
        val savedFaceUrl = intent.getStringExtra("SAVED_FACE_URL")

        if (isLoginMode && savedFaceUrl != null) {
            verifyFace(savedFaceUrl)
        } else {
            submitEkyc()
        }
    }

    private fun verifyFace(savedUrl: String) {
        if (faceBitmap == null) {
            Toast.makeText(this, "Vui lòng chụp ảnh", Toast.LENGTH_SHORT).show()
            return
        }

        btnSubmit.isEnabled = false
        btnSubmit.text = "Đang xác thực..."

        // Giả lập delay đối chiếu 2 giây
        imgFace.postDelayed({
            val isMatched = true // Demo luôn đúng

            if (isMatched) {
                setResult(RESULT_OK) // Trả kết quả thành công cho TransferActivity
                finish()
            } else {
                btnSubmit.isEnabled = true
                Toast.makeText(this, "Khuôn mặt không khớp!", Toast.LENGTH_SHORT).show()
            }
        }, 2000)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Cần quyền Camera để tiếp tục", Toast.LENGTH_LONG).show()
            }
        }
    }
}
