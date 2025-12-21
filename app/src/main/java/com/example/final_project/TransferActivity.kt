package com.example.final_project

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import kotlin.random.Random
import android.content.Intent


class TransferActivity : AppCompatActivity() {

    private lateinit var edtReceiverAccountNumber: EditText
    private lateinit var btnCheckReceiver: Button
    private lateinit var tvReceiverName: TextView
    private lateinit var tvBalance: TextView
    private lateinit var edtTransferAmount: EditText
    private lateinit var edtTransferContent: EditText
    private lateinit var btnConfirmTransfer: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var receiverAccountRef: DocumentReference? = null
    private var isReceiverChecked = false
    
    private var currentOTP: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()
        initActions()
        loadSenderBalance()
    }

    private fun initViews() {
        edtReceiverAccountNumber = findViewById(R.id.edtReceiverAccountNumber)
        btnCheckReceiver = findViewById(R.id.btnCheckReceiver)
        tvReceiverName = findViewById(R.id.tvReceiverName)
        edtTransferAmount = findViewById(R.id.edtTransferAmount)
        edtTransferContent = findViewById(R.id.edtTransferContent)
        btnConfirmTransfer = findViewById(R.id.btnConfirmTransfer)
        tvBalance = findViewById(R.id.tvBalance)
    }

    private fun initActions() {
        btnCheckReceiver.setOnClickListener { checkReceiver() }
        btnConfirmTransfer.setOnClickListener { initiateTransferProcess() }
    }

    private fun loadSenderBalance() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("accounts")
            .whereEqualTo("uid", uid)
            .whereEqualTo("type", "checking")
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val balance = snapshot.documents[0].getDouble("balance") ?: 0.0
                    tvBalance.text = "${String.format("%,.0f", balance)} VNĐ"
                } else {
                    tvBalance.text = "0 VNĐ"
                }
            }
            .addOnFailureListener {
                tvBalance.text = "Số dư: --"
            }
    }

    private fun checkReceiver() {
        val receiverAccountIdString = edtReceiverAccountNumber.text.toString()
        if (receiverAccountIdString.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tài khoản người nhận", Toast.LENGTH_SHORT).show()
            return
        }
        val receiverAccountId = receiverAccountIdString.trim()
        if (receiverAccountId.isEmpty()) {
            Toast.makeText(this, "Số tài khoản không hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("accounts")
            .whereEqualTo("type", "checking")
            .whereEqualTo("accountId", receiverAccountId)
            .limit(1)
            .get()
            .addOnSuccessListener { accountSnapshot ->
                if (accountSnapshot.isEmpty) {
                    Toast.makeText(this, "Số tài khoản người nhận không tồn tại", Toast.LENGTH_SHORT).show()
                    isReceiverChecked = false
                    tvReceiverName.visibility = View.GONE
                    return@addOnSuccessListener
                }
                val receiverAccountDoc = accountSnapshot.documents[0]
                val receiverUid = receiverAccountDoc.getString("uid") ?: ""

                if (receiverUid == auth.currentUser?.uid) {
                    Toast.makeText(this, "Không thể tự chuyển tiền cho chính mình", Toast.LENGTH_LONG).show()
                    isReceiverChecked = false
                    tvReceiverName.visibility = View.GONE
                    return@addOnSuccessListener
                }

                receiverAccountRef = receiverAccountDoc.reference

                db.collection("customers").document(receiverUid).get()
                    .addOnSuccessListener { customerDoc ->
                        if (customerDoc.exists()) {
                            val receiverName = customerDoc.getString("name") ?: "N/A"
                            tvReceiverName.text = "Tên người nhận: $receiverName"
                            tvReceiverName.visibility = View.VISIBLE
                            isReceiverChecked = true
                        } else {
                            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin chủ tài khoản", Toast.LENGTH_SHORT).show()
                            isReceiverChecked = false
                        }
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi khi kiểm tra: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun initiateTransferProcess() {
        if (!isReceiverChecked || receiverAccountRef == null) {
            Toast.makeText(this, "Vui lòng kiểm tra thông tin người nhận trước", Toast.LENGTH_SHORT).show()
            return
        }

        val amountString = edtTransferAmount.text.toString()
        if (amountString.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show()
            return
        }
        val transferAmount = edtTransferAmount.text.toString().toDoubleOrNull() ?: 0.0

        val uid = auth.currentUser?.uid ?: return
        db.collection("accounts")
            .whereEqualTo("uid", uid)
            .whereEqualTo("type", "checking")
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val balance = snapshot.documents[0].getDouble("balance") ?: 0.0
                    if (balance < transferAmount) {
                        Toast.makeText(this, "Số dư không đủ", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }

                    if (transferAmount >= 10000000) {
                        // Nếu >= 10 triệu, bắt buộc quét mặt trước
                        startFaceVerification()
                    } else {
                        sendOTPAndShowDialog()
                    }
                }
            }
    }
    private fun startFaceVerification() {
        val uid = auth.currentUser?.uid ?: return
        // Lấy thông tin faceImageURL của user hiện tại để đối chiếu
        db.collection("customers").document(uid).get()
            .addOnSuccessListener { doc ->
                val savedFaceUrl = doc.getString("faceImageURL")
                if (savedFaceUrl.isNullOrEmpty()) {
                    Toast.makeText(this, "Bạn chưa đăng ký eKYC khuôn mặt để thực hiện giao dịch lớn", Toast.LENGTH_LONG).show()
                } else {
                    val intent = Intent(this, EkycActivity::class.java)
                    intent.putExtra("IS_LOGIN_MODE", true) // Dùng mode xác thực
                    intent.putExtra("SAVED_FACE_URL", savedFaceUrl)
                    // Dùng ActivityResultLauncher hoặc startActivityForResult
                    startActivityForResult(intent, 999)
                }
            }
    }
    
    private fun sendOTPAndShowDialog() {
        val currentUser = auth.currentUser
        val userEmail = currentUser?.email
        
        if (userEmail == null) {
            Toast.makeText(this, "Không tìm thấy email của bạn để gửi OTP", Toast.LENGTH_LONG).show()
            return
        }
        
        currentOTP = Random.nextInt(100000, 999999).toString()
        
        Toast.makeText(this, "Đang gửi mã OTP đến $userEmail...", Toast.LENGTH_SHORT).show()
        
        lifecycleScope.launch {
            val isSent = GMailSender.sendOTPEmail(userEmail, currentOTP)
            if (isSent) {
                Toast.makeText(this@TransferActivity, "Đã gửi OTP thành công! Vui lòng kiểm tra email.", Toast.LENGTH_LONG).show()
                showOTPDialog()
            } else {
                Toast.makeText(this@TransferActivity, "Gửi OTP thất bại. Vui lòng kiểm tra kết nối mạng.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 999 && resultCode == Activity.RESULT_OK) {
            // Quét mặt thành công -> Tiếp tục bước gửi OTP
            Toast.makeText(this, "Xác thực khuôn mặt thành công!", Toast.LENGTH_SHORT).show()
            sendOTPAndShowDialog()
        }
    }
    
    private fun showOTPDialog() {
        val inputEditTextField = EditText(this)
        inputEditTextField.hint = "Nhập mã 6 số"
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("Xác thực giao dịch")
            .setMessage("Mã OTP đã được gửi đến email của bạn.\nVui lòng nhập mã để xác nhận chuyển tiền.")
            .setView(inputEditTextField)
            .setCancelable(false)
            .setPositiveButton("Xác nhận") { _, _ ->
                val enteredOTP = inputEditTextField.text.toString()
                if (enteredOTP == currentOTP) {
                    performTransfer()
                } else {
                    Toast.makeText(this, "Mã OTP không chính xác!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy bỏ", null)
            .create()
        dialog.show()
    }

    private fun performTransfer() {
        val amountString = edtTransferAmount.text.toString()
        val transferAmount = amountString.toDouble()
        val content = edtTransferContent.text.toString().ifEmpty { "Chuyen khoan noi bo" }
        val senderId = auth.currentUser?.uid ?: return

        db.collection("accounts")
            .whereEqualTo("uid", senderId)
            .whereEqualTo("type", "checking")
            .limit(1)
            .get()
            .addOnSuccessListener { senderAccountSnapshot ->
                if (senderAccountSnapshot.isEmpty) {
                    Toast.makeText(this, "Lỗi: Không tìm thấy tài khoản thanh toán của bạn.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                val senderAccountRef = senderAccountSnapshot.documents[0].reference

                if (senderAccountRef.path == receiverAccountRef?.path) {
                    Toast.makeText(this, "Thao tác không hợp lệ.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                db.runTransaction { transaction ->

                    val senderSnap = transaction.get(senderAccountRef)
                    val receiverSnap = transaction.get(receiverAccountRef!!)

                    val senderBalance = senderSnap.getDouble("balance") ?: 0.0
                    if (senderBalance < transferAmount) {
                        throw Exception("Số dư không đủ.")
                    }

                    val receiverBalance = receiverSnap.getDouble("balance") ?: 0.0

                    val fromAccountId = senderSnap.getString("accountId")!!
                    val toAccountId = receiverSnap.getString("accountId")!!
                    val receiverUid = receiverSnap.getString("uid")!!

                    transaction.update(senderAccountRef, "balance", senderBalance - transferAmount)
                    transaction.update(receiverAccountRef!!, "balance", receiverBalance + transferAmount)

                    val time = Timestamp.now()

                    val senderTransaction = hashMapOf(
                        "transactionId" to "T${System.currentTimeMillis()}_OUT",
                        "uid" to senderId,
                        "fromAccountId" to fromAccountId,
                        "toAccountId" to toAccountId,
                        "amount" to transferAmount,
                        "note" to content,
                        "status" to "Thành công",
                        "timestamp" to time,
                        "type" to "internal_transfer_out"
                    )

                    val receiverTransaction = hashMapOf(
                        "transactionId" to "T${System.currentTimeMillis()}_IN",
                        "uid" to receiverUid,
                        "fromAccountId" to fromAccountId,
                        "toAccountId" to toAccountId,
                        "amount" to transferAmount,
                        "note" to content,
                        "status" to "Thành công",
                        "timestamp" to time,
                        "type" to "internal_transfer_in"
                    )

                    transaction.set(db.collection("transactions").document(), senderTransaction)
                    transaction.set(db.collection("transactions").document(), receiverTransaction)

                    null
                }.addOnSuccessListener {
                    Toast.makeText(this, "Chuyển khoản thành công!", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Giao dịch thất bại: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                 Toast.makeText(this, "Lỗi khi tìm tài khoản của bạn: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
