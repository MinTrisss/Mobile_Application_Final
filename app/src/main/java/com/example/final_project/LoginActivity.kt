package com.example.final_project

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent
import android.util.Log
import android.widget.ImageButton


class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var  btnFaceLogin: ImageButton

    private val db = FirebaseFirestore.getInstance()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_page)

        Log.d("LOGIN", "LoginActivity loaded")

        auth = FirebaseAuth.getInstance()
        auth.firebaseAuthSettings
            .setAppVerificationDisabledForTesting(true)
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        btnLogin = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {

            val input = edtEmail.text.toString().trim()   // email OR phone
            val password = edtPassword.text.toString().trim()

            if (input.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email/SĐT không được để trống!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (input.contains("@")) {
                loginWithEmail(input, password)
            }
            else {
                loginWithPhone(input, password)
            }
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loginWithEmail(email: String, password: String) {

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                logLoginHistory()
                checkUserRoleAndNavigate()
            }
            .addOnFailureListener {
                Toast.makeText(this, it.message ?: "Đăng nhập thất bại!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loginWithPhone(phone: String, password: String) {

        db.collection("customers")
            .whereEqualTo("phoneNum", phone)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->

                if (snap.isEmpty) {
                    Toast.makeText(this, "Số điện thoại không tồn tại!", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val email = snap.documents[0].getString("email")
                if (email.isNullOrEmpty()) {
                    Toast.makeText(this, "Email không tồn tại!", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        logLoginHistory()
                        checkUserRoleAndNavigate()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, it.message ?: "Đăng nhập thất bại", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Đăng nhập thất bại", Toast.LENGTH_SHORT).show()
            }
    }

    private fun logLoginHistory() {

        val currentUser = auth.currentUser ?: return

        val logData = hashMapOf(
            "uid" to currentUser.uid,
            "email" to currentUser.email,
            "loginTime" to com.google.firebase.Timestamp.now()
        )

        db.collection("loginHistory")
            .add(logData)
            .addOnFailureListener {
                Log.e("LOGIN_HISTORY", "Failed to log login", it)
            }
    }

    private fun checkUserRoleAndNavigate() {
        val uid = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("employees").document(uid).get()
            .addOnSuccessListener { employeeDoc ->

                if (employeeDoc.exists()) {
                    // OPTIONAL: check locked
                    val status = employeeDoc.getString("status") ?: "Hoạt động"
                    if (status == "Đã khoá") {
                        auth.signOut()
                        Toast.makeText(this, "Tài khoản đã bị khoá!", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }

                    navigateToEmployeeScreen()
                    return@addOnSuccessListener
                }

                db.collection("customers").document(uid).get()
                    .addOnSuccessListener { customerDoc ->

                        if (customerDoc.exists()) {

                            val status = customerDoc.getString("status") ?: "Hoạt động"
                            if (status == "Đã khoá") {
                                auth.signOut()
                                Toast.makeText(this, "Tài khoản đã bị khoá!", Toast.LENGTH_LONG).show()
                                return@addOnSuccessListener
                            }

                            val ekycStatus = customerDoc.getString("ekycStatus") ?: "pending"
                            val faceUrl = customerDoc.getString("faceImageURL")

                            if (ekycStatus != "verified") {
                                navigateToEkycScreen(faceUrl)
                            } else {
                                navigateToCustomerScreen()
                            }

                        } else {
                            auth.signOut()
                            Toast.makeText(this, "Không tìm thấy tài khoản!", Toast.LENGTH_LONG).show()
                        }
                    }
                    .addOnFailureListener {
                        auth.signOut()
                        Toast.makeText(this, "Không thể tải thông tin khách hàng!", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                auth.signOut()
                Toast.makeText(this, "Không thể tải thông tin nhân viên!", Toast.LENGTH_SHORT).show()
            }
    }

    // Trong LoginActivity.kt
    private fun startFaceLogin(identifier: String) {
        val query = if (identifier.contains("@")) {
            db.collection("customers").whereEqualTo("email", identifier)
        } else {
            db.collection("customers").whereEqualTo("phoneNum", identifier)
        }

        // Trong LoginActivity.kt -> hàm startFaceLogin
        query.get().addOnSuccessListener { snap ->
            if (!snap.isEmpty) {
                val doc = snap.documents[0]
                val email = doc.getString("email")
                val savedFaceUrl = doc.getString("faceImageURL")

                val intent = Intent(this, EkycActivity::class.java)
                intent.putExtra("IS_LOGIN_MODE", true)
                intent.putExtra("SAVED_FACE_URL", savedFaceUrl)
                intent.putExtra("LOGIN_EMAIL", email) // QUAN TRỌNG: Truyền email sang
                startActivity(intent)
            }
        }
    }

    // Trong LoginActivity.kt
    private fun navigateToEkycScreen(savedFaceUrl: String?) {
        val intent = Intent(this, EkycActivity::class.java)
        // Nếu chưa verified, ta truyền mode "REGISTRATION" (đăng ký ảnh)
        intent.putExtra("IS_LOGIN_MODE", false)
        intent.putExtra("SAVED_FACE_URL", savedFaceUrl)
        startActivity(intent)
        finish()
    }
    private fun navigateToEmployeeScreen() {
        Toast.makeText(applicationContext, "Bạn đã đăng nhập thành công.", Toast.LENGTH_LONG).show()
        val intent = Intent(this, EmployeeHomeActivity::class.java)
        intent.putExtra("userRole", "employee")
        startActivity(intent)
        finish()
    }

    private fun navigateToCustomerScreen() {
        Toast.makeText(applicationContext, "Bạn đã đăng nhập thành công.", Toast.LENGTH_LONG).show()
        val intent = Intent(this, CustomerHomeActivity::class.java)
        intent.putExtra("userRole", "customer")
        startActivity(intent)
        finish()
    }

}
