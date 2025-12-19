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

class DepositWithdrawActivity : AppCompatActivity() {

    // UI Components
    private lateinit var edtReceiverAccountNumber: EditText
    private lateinit var btnCheckReceiver: Button
    private lateinit var tvReceiverName: TextView
    private lateinit var tvBalance: TextView
    private lateinit var edtTransferAmount: EditText
    private lateinit var btnWithdraw: Button
    private lateinit var btnDeposit: Button
    private var checkedAccountRef: DocumentReference? = null
    private var checkedAccountBalance: Double = 0.0
    private var checkedAccountId: String = ""



    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // State variables
    private var isReceiverChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deposit_withdraw)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()
        initActions()
    }

    private fun initViews() {
        edtReceiverAccountNumber = findViewById(R.id.edtReceiverAccountNumber)
        btnCheckReceiver = findViewById(R.id.btnCheckReceiver)
        tvReceiverName = findViewById(R.id.tvReceiverName)
        edtTransferAmount = findViewById(R.id.edtTransferAmount)
        tvBalance = findViewById(R.id.tvBalance)
        btnWithdraw = findViewById(R.id.btnWithdraw)
        btnDeposit = findViewById(R.id.btnDeposit)
    }

    private fun initActions() {
        btnCheckReceiver.setOnClickListener { checkReceiver() }
        btnWithdraw.setOnClickListener { performWithdraw() }
        btnDeposit.setOnClickListener { performDeposit() }
    }

    private fun checkReceiver() {

        val accountIdInput = edtReceiverAccountNumber.text.toString().trim()
        if (accountIdInput.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tài khoản", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("accounts")
            .whereEqualTo("type", "checking")
            .whereEqualTo("accountId", accountIdInput)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->

                // ❗ BẮT BUỘC CHECK RỖNG TRƯỚC
                if (snapshot.isEmpty) {
                    Toast.makeText(this, "Số tài khoản không tồn tại", Toast.LENGTH_SHORT).show()
                    isReceiverChecked = false
                    checkedAccountRef = null
                    tvReceiverName.visibility = View.GONE
                    tvBalance.text = "--"
                    return@addOnSuccessListener
                }

                val accountDoc = snapshot.documents[0]
                val receiverUid = accountDoc.getString("uid") ?: ""

                // ✅ LƯU STATE ACCOUNT
                checkedAccountRef = accountDoc.reference
                checkedAccountBalance = accountDoc.getDouble("balance") ?: 0.0
                checkedAccountId = accountDoc.getString("accountId") ?: ""

                // ✅ HIỂN THỊ SỐ DƯ
                tvBalance.text =
                    "${String.format("%,.0f", checkedAccountBalance)} VNĐ"

                // Load tên chủ tài khoản
                db.collection("customers")
                    .document(receiverUid)
                    .get()
                    .addOnSuccessListener { customerDoc ->
                        if (customerDoc.exists()) {
                            val name = customerDoc.getString("name") ?: "N/A"
                            tvReceiverName.text = "Chủ tài khoản: $name"
                            tvReceiverName.visibility = View.VISIBLE
                            isReceiverChecked = true
                        } else {
                            Toast.makeText(
                                this,
                                "Không tìm thấy thông tin chủ tài khoản",
                                Toast.LENGTH_SHORT
                            ).show()
                            isReceiverChecked = false
                        }
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi khi kiểm tra: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun performWithdraw() {

        if (!isReceiverChecked || checkedAccountRef == null) {
            Toast.makeText(this, "Vui lòng kiểm tra tài khoản trước", Toast.LENGTH_SHORT).show()
            return
        }

        val amountString = edtTransferAmount.text.toString()
        if (amountString.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show()
            return
        }

        val withdrawAmount = amountString.toDoubleOrNull()
        if (withdrawAmount == null || withdrawAmount <= 0) {
            Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }

        db.runTransaction { transaction ->

            val accountSnap = transaction.get(checkedAccountRef!!)
            val balance = accountSnap.getDouble("balance") ?: 0.0

            if (balance < withdrawAmount) {
                throw Exception("Số dư không đủ")
            }

            // 1️⃣ Trừ tiền
            transaction.update(
                checkedAccountRef!!,
                "balance",
                balance - withdrawAmount
            )

            // 2️⃣ Ghi lịch sử giao dịch
            val withdrawTransaction = hashMapOf(
                "transactionId" to "W${System.currentTimeMillis()}",
                "uid" to accountSnap.getString("uid"),
                "fromAccountId" to checkedAccountId,
                "toAccountId" to "",
                "amount" to withdrawAmount,
                "note" to "Rút tiền",
                "status" to "Thành công",
                "timestamp" to Timestamp.now(),
                "type" to "withdraw"
            )

            transaction.set(
                db.collection("transactions").document(),
                withdrawTransaction
            )

            null
        }
            .addOnSuccessListener {
                Toast.makeText(this, "Rút tiền thành công", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, e.message ?: "Giao dịch thất bại", Toast.LENGTH_LONG).show()
            }
    }

    private fun performDeposit() {

        if (!isReceiverChecked || checkedAccountRef == null) {
            Toast.makeText(this, "Vui lòng kiểm tra tài khoản trước", Toast.LENGTH_SHORT).show()
            return
        }

        val amountString = edtTransferAmount.text.toString()
        if (amountString.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show()
            return
        }

        val depositAmount = amountString.toDoubleOrNull()
        if (depositAmount == null || depositAmount <= 0) {
            Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }

        db.runTransaction { transaction ->

            val accountSnap = transaction.get(checkedAccountRef!!)
            val balance = accountSnap.getDouble("balance") ?: 0.0

            // 1️⃣ Trừ tiền
            transaction.update(
                checkedAccountRef!!,
                "balance",
                balance + depositAmount
            )

            // 2️⃣ Ghi lịch sử giao dịch
            val withdrawTransaction = hashMapOf(
                "transactionId" to "W${System.currentTimeMillis()}",
                "uid" to accountSnap.getString("uid"),
                "fromAccountId" to checkedAccountId,
                "toAccountId" to "",
                "amount" to depositAmount,
                "note" to "Nạp tiền",
                "status" to "Thành công",
                "timestamp" to Timestamp.now(),
                "type" to "deposit"
            )

            transaction.set(
                db.collection("transactions").document(),
                withdrawTransaction
            )

            null
        }
            .addOnSuccessListener {
                Toast.makeText(this, "Nạp tiền thành công", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, e.message ?: "Giao dịch thất bại", Toast.LENGTH_LONG).show()
            }
    }
}
