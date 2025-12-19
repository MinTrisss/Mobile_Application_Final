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
import android.util.Log
import android.widget.TextView
import android.widget.AdapterView
import androidx.core.widget.addTextChangedListener
import android.view.View


class OpenSavingAccountActivity : AppCompatActivity() {

    private lateinit var edtSavingAmount: EditText
    private lateinit var spinnerSavingTerm: Spinner
    private lateinit var btnOpenSavingAccount: Button
    private lateinit var tvMonthlyInterest: TextView
    private lateinit var tvTotalInterest: TextView
    private lateinit var tvTotalAtMaturity: TextView

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

        setupInterestPreviewListeners()

    }

    private fun initViews() {
        edtSavingAmount = findViewById(R.id.edtSavingAmount)
        spinnerSavingTerm = findViewById(R.id.spinnerSavingTerm)
        btnOpenSavingAccount = findViewById(R.id.btnOpenSavingAccount)
        tvMonthlyInterest = findViewById(R.id.tvMonthlyInterest)
        tvTotalInterest = findViewById(R.id.tvTotalInterest)
        tvTotalAtMaturity = findViewById(R.id.tvTotalAtMaturity)
    }

    private fun setupSpinner() {
        val terms = arrayOf("6 tháng", "1 năm", "2 năm", "3 năm")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, terms)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSavingTerm.adapter = adapter
    }

    private fun initActions() {
        btnOpenSavingAccount.setOnClickListener {
            Toast.makeText(this, "CLICK OK", Toast.LENGTH_SHORT).show()
            Log.d("DEBUG", "CLICK OK")
            performSavingTransaction()
        }
    }
    private fun getTermMonths(term: String): Int {
        return when (term) {
            "6 tháng" -> 6
            "1 năm" -> 12
            "2 năm" -> 24
            "3 năm" -> 36
            else -> 0
        }
    }

    private fun getInterestRate(term: String): Double {
        return when (term) {
            "6 tháng" -> 0.065
            "1 năm" -> 0.07
            "2 năm" -> 0.075
            "3 năm" -> 0.08
            else -> 0.06
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
                    val calc = calculateSaving(savingAmount, selectedTerm)

                    val newSavingAccount = hashMapOf(
                        "accountId" to newSavingAccountId,
                        "uid" to uid,
                        "type" to "saving",
                        "createdAt" to Timestamp.now(),

                        "term" to calc.term,
                        "principal" to calc.principal,

                        "interestRate" to calc.interestRate,
                        "monthlyInterest" to calc.monthlyInterest,
                        "termMonths" to calc.termMonths,
                        "totalInterest" to calc.totalInterest,
                        "totalAtMaturity" to calc.totalAtMaturity
                    )


                    val newBalance = currentBalance - savingAmount
                    transaction.update(checkingAccountRef, "balance", newBalance)
                    transaction.set(db.collection("accounts").document(newSavingAccountId), newSavingAccount)

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

    private fun updateInterestPreview() {
        val amount = edtSavingAmount.text.toString().toDoubleOrNull() ?: return
        if (amount < MINIMUM_SAVING_AMOUNT) return

        val term = spinnerSavingTerm.selectedItem.toString()
        val calc = calculateSaving(amount, term)

        tvMonthlyInterest.text =
            "Lãi hàng tháng: ${String.format("%,.0f", calc.monthlyInterest)} VNĐ"

        tvTotalInterest.text =
            "Tổng lãi: ${String.format("%,.0f", calc.totalInterest)} VNĐ"

        tvTotalAtMaturity.text =
            "Tổng nhận khi đáo hạn: ${String.format("%,.0f", calc.totalAtMaturity)} VNĐ"
    }

    private fun setupInterestPreviewListeners() {

        edtSavingAmount.addTextChangedListener {
            updateInterestPreview()
        }

        spinnerSavingTerm.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    updateInterestPreview()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
    }

    private fun calculateSaving(amount: Double, term: String): SavingCalculation {
        val termMonths = getTermMonths(term)
        val rate = getInterestRate(term)

        val monthlyInterest = amount * rate / 12
        val totalInterest = monthlyInterest * termMonths
        val totalAtMaturity = amount + totalInterest

        return SavingCalculation(
            principal = amount,
            term = term,
            termMonths = termMonths,
            interestRate = rate,
            monthlyInterest = monthlyInterest,
            totalInterest = totalInterest,
            totalAtMaturity = totalAtMaturity
        )
    }



    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
