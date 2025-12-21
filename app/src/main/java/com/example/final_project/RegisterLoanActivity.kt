package com.example.final_project

import android.app.Activity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
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
    private lateinit var tvLoanInterest: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var interestRates: Map<String, Double> = emptyMap()

    private val MINIMUM_LOAN_AMOUNT = 5_000_000.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_loan)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()
        setupSpinner()
        loadInterestRates()
        initActions()
    }

    private fun initViews() {
        edtLoanAmount = findViewById(R.id.edtLoanAmount)
        spinnerLoanDuration = findViewById(R.id.spinnerLoanDuration)
        btnRegisterLoanConfirm = findViewById(R.id.btnRegisterLoanConfirm)
        tvLoanInterest = findViewById(R.id.tvLoanInterest)
    }

    private fun setupSpinner() {
        val durations = arrayOf("1 năm", "2 năm", "3 năm", "5 năm")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, durations)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLoanDuration.adapter = adapter

        spinnerLoanDuration.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedDuration = durations[position]
                    showInterestRate(selectedDuration)
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
    }


    private fun getDurationMonths(duration: String): Int {
        return when (duration) {
            "1 năm" -> 12
            "2 năm" -> 24
            "3 năm" -> 36
            "5 năm" -> 60
            else -> 0
        }
    }


    private fun initActions() {
        btnRegisterLoanConfirm.setOnClickListener {
            submitLoanApplication()
        }
    }

    private fun loadInterestRates() {
        btnRegisterLoanConfirm.isEnabled = false

        db.collection("interest_rates")
            .document("loan")
            .get()
            .addOnSuccessListener { doc ->
                interestRates = doc.data
                    ?.filterValues { it is Double }
                    ?.mapValues { it.value as Double }
                    ?: emptyMap()

                if (interestRates.isNotEmpty()) {
                    btnRegisterLoanConfirm.isEnabled = true
                    val selectedDuration = spinnerLoanDuration.selectedItem.toString()
                    showInterestRate(selectedDuration)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Không tải được lãi suất", Toast.LENGTH_SHORT).show()
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
        val durationMonths = getDurationMonths(selectedDuration)
        val interestRate = interestRates[selectedDuration]
        if (interestRate == null || interestRate <= 0) {
            Toast.makeText(this, "Không tìm thấy lãi suất cho kỳ hạn $selectedDuration", Toast.LENGTH_SHORT).show()
            return
        }

        val monthlyPayment = calculatePeriodicPayment(
            principal = loanAmount,
            annualRate = interestRate,
            termMonths = durationMonths,
            frequency = PaymentFrequency.MONTHLY
        )

        val totalPayment = monthlyPayment * durationMonths
        val totalInterest = totalPayment - loanAmount


        val newLoanApplication = hashMapOf(
            "accountId" to newLoanAccountId,
            "uid" to uid,
            "type" to "loan",

            "principal" to loanAmount,
            "interestRate" to interestRate,
            "termMonths" to durationMonths,

            "monthlyPayment" to monthlyPayment,
            "totalPayment" to totalPayment,
            "totalInterest" to totalInterest,

            "status" to "Chờ duyệt",
            "createdAt" to Timestamp.now()
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

    private fun showInterestRate(duration: String) {
        val rate = interestRates[duration]

        if (rate != null) {
            tvLoanInterest.text = "Lãi suất: ${(rate * 100).toInt()}% / năm"
        } else {
            tvLoanInterest.text = "Lãi suất: --"
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
