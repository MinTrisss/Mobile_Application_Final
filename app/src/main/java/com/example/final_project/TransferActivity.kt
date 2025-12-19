package com.example.final_project

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

class TransferActivity : AppCompatActivity() {

    // UI Components
    private lateinit var edtReceiverAccountNumber: EditText
    private lateinit var btnCheckReceiver: Button
    private lateinit var tvReceiverName: TextView
    private lateinit var tvBalance: TextView
    private lateinit var edtTransferAmount: EditText
    private lateinit var edtTransferContent: EditText
    private lateinit var btnConfirmTransfer: Button

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // State variables
    private var receiverAccountRef: DocumentReference? = null
    private var isReceiverChecked = false

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
        btnConfirmTransfer.setOnClickListener { performTransfer() }
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


        // Find the receiver's checking account
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

                // **KIỂM TRA CHUYỂN TIỀN CHO CHÍNH MÌNH**
                if (receiverUid == auth.currentUser?.uid) {
                    Toast.makeText(this, "Không thể tự chuyển tiền cho chính mình", Toast.LENGTH_LONG).show()
                    isReceiverChecked = false
                    tvReceiverName.visibility = View.GONE
                    return@addOnSuccessListener
                }

                receiverAccountRef = receiverAccountDoc.reference

                // Find the customer's name
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

    private fun performTransfer() {
        if (!isReceiverChecked || receiverAccountRef == null) {
            Toast.makeText(this, "Vui lòng kiểm tra thông tin người nhận trước", Toast.LENGTH_SHORT).show()
            return
        }

        val amountString = edtTransferAmount.text.toString()
        if (amountString.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show()
            return
        }
        val transferAmount = amountString.toDoubleOrNull()
        if (transferAmount == null || transferAmount <= 0) {
            Toast.makeText(this, "Số tiền chuyển phải lớn hơn 0", Toast.LENGTH_SHORT).show()
            return
        }

        val content = edtTransferContent.text.toString().ifEmpty { "Chuyen khoan noi bo" }
        val senderId = auth.currentUser?.uid ?: return

        // Find sender's checking account first
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

                // **Đảm bảo không tự chuyển cho mình lần nữa**
                if (senderAccountRef.path == receiverAccountRef?.path) {
                    Toast.makeText(this, "Thao tác không hợp lệ.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Run the transaction
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

                    // 1️⃣ UPDATE BALANCE
                    transaction.update(senderAccountRef, "balance", senderBalance - transferAmount)
                    transaction.update(receiverAccountRef!!, "balance", receiverBalance + transferAmount)

                    val time = Timestamp.now()

                    // 2️⃣ TRANSACTION – NGƯỜI GỬI (TRỪ TIỀN)
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

                    // 3️⃣ TRANSACTION – NGƯỜI NHẬN (CỘNG TIỀN)
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
                    setResult(Activity.RESULT_OK) // Trả về kết quả để màn hình Home biết cần cập nhật
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
