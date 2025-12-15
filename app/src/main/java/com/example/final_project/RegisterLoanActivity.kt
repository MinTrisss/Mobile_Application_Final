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

class RegisterLoanActivity : AppCompatActivity() {

    private lateinit var edtLoanAmount: EditText
    private lateinit var spinnerLoanDuration: Spinner
    private lateinit var btnRegisterLoanConfirm: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val MINIMUM_LOAN_AMOUNT = 5_000_000.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_loan)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()
        setupSpinner()
        initActions()
    }

    private fun initViews() {
        edtLoanAmount = findViewById(R.id.edtLoanAmount)
        spinnerLoanDuration = findViewById(R.id.spinnerLoanDuration)
        btnRegisterLoanConfirm = findViewById(R.id.btnRegisterLoanConfirm)
    }

    private fun setupSpinner() {
        val durations = arrayOf("1 năm", "2 năm", "3 năm", "5 năm")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, durations)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLoanDuration.adapter = adapter
    }

    private fun initActions() {
        btnRegisterLoanConfirm.setOnClickListener {
            submitLoanApplication()
        }
    }

    private fun submitLoanApplication() {
        val amountString = edtLoanAmount.text.toString()
        if (amountString.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền cần vay", Toast.LENGTH_SHORT).show()
            return
        }

        val loanAmount = amountString.toDoubleOrNull()
        if (loanAmount == null || loanAmount < MINIMUM_LOAN_AMOUNT) {
            Toast.makeText(this, "Số tiền vay tối thiểu là ${String.format("%,.0f", MINIMUM_LOAN_AMOUNT)} VNĐ", Toast.LENGTH_LONG).show()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Không thể xác thực người dùng.", Toast.LENGTH_SHORT).show()
            return
        }
        val uid = currentUser.uid

        val newLoanAccountId = ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L).toString() // Chuyển sang String
        val selectedDuration = spinnerLoanDuration.selectedItem.toString()

        val newLoanApplication = hashMapOf(
            "accountId" to newLoanAccountId,
            "uid" to uid,
            "type" to "loan",
            "status" to "Chờ duyệt",
            "createdAt" to Timestamp.now(),
            "duration" to selectedDuration,
            "loan" to loanAmount
        )

        db.collection("accounts")
            .add(newLoanApplication)
            .addOnSuccessListener {
                Toast.makeText(this, "Đơn đăng ký vay của bạn đã được gửi và đang chờ xét duyệt.", Toast.LENGTH_LONG).show()
                setResult(Activity.RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gửi đơn đăng ký thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
