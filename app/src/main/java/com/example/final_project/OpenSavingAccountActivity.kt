package com.example.final_project

import android.app.Activity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.util.concurrent.ThreadLocalRandom

class OpenSavingAccountActivity : AppCompatActivity() {

    private lateinit var edtSavingAmount: EditText
    private lateinit var spinnerSavingTerm: Spinner
    private lateinit var btnOpenSavingAccount: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val MINIMUM_SAVING_AMOUNT = 1_000_000.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_saving_account)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()
        setupSpinner()
        initActions()
    }

    private fun initViews() {
        edtSavingAmount = findViewById(R.id.edtSavingAmount)
        spinnerSavingTerm = findViewById(R.id.spinnerSavingTerm)
        btnOpenSavingAccount = findViewById(R.id.btnOpenSavingAccount)
    }

    private fun setupSpinner() {
        val terms = arrayOf("6 tháng", "1 năm", "2 năm", "3 năm")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, terms)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSavingTerm.adapter = adapter
    }

    private fun initActions() {
        btnOpenSavingAccount.setOnClickListener {
            performSavingTransaction()
        }
    }

    private fun performSavingTransaction() {
        val amountString = edtSavingAmount.text.toString()
        if (amountString.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền gửi", Toast.LENGTH_SHORT).show()
            return
        }

        val savingAmount = amountString.toDoubleOrNull()
        if (savingAmount == null || savingAmount < MINIMUM_SAVING_AMOUNT) {
            Toast.makeText(this, "Số tiền gửi tối thiểu là ${String.format("%,.0f", MINIMUM_SAVING_AMOUNT)} VNĐ", Toast.LENGTH_LONG).show()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Không thể xác thực người dùng.", Toast.LENGTH_SHORT).show()
            return
        }
        val uid = currentUser.uid

        db.collection("accounts")
            .whereEqualTo("uid", uid)
            .whereEqualTo("type", "checking")
            .limit(1)
            .get()
            .addOnSuccessListener { checkingAccountSnapshot ->
                if (checkingAccountSnapshot.isEmpty) {
                    Toast.makeText(this, "Lỗi: Không tìm thấy tài khoản thanh toán của bạn.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                
                val checkingAccountRef = checkingAccountSnapshot.documents[0].reference

                db.runTransaction { transaction ->
                    val checkingSnap = transaction.get(checkingAccountRef)
                    val currentBalance = checkingSnap.getDouble("balance") ?: 0.0

                    if (currentBalance < savingAmount) {
                        throw Exception("Số dư tài khoản thanh toán không đủ.")
                    }

                    val newSavingAccountId = ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L).toString() // Chuyển sang String
                    val selectedTerm = spinnerSavingTerm.selectedItem.toString()
                    val newSavingAccount = hashMapOf(
                        "accountId" to newSavingAccountId,
                        "uid" to uid,
                        "type" to "saving",
                        "createdAt" to Timestamp.now(),
                        "Term" to selectedTerm,
                        "saving" to savingAmount
                    )

                    val newBalance = currentBalance - savingAmount
                    transaction.update(checkingAccountRef, "balance", newBalance)
                    transaction.set(db.collection("accounts").document(), newSavingAccount)

                    null
                }.addOnSuccessListener {
                    Toast.makeText(this, "Mở tài khoản tiết kiệm thành công!", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Giao dịch thất bại: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi khi tìm tài khoản thanh toán: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
