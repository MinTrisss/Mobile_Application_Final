package com.example.final_project

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Button
import android.widget.Toast
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions


class EditInterestRateActivity : AppCompatActivity() {

    private lateinit var spinnerTerm: Spinner
    private lateinit var edtInterestRate: EditText
    private lateinit var btnSaveRate: Button
    private lateinit var spinnerTerm1: Spinner
    private lateinit var edtInterestRateLoan: EditText
    private lateinit var btnSaveRateLoan: Button

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_interest_rate)

        spinnerTerm = findViewById(R.id.spinnerTerm)
        edtInterestRate = findViewById(R.id.edtInterestRate)
        btnSaveRate = findViewById(R.id.btnSaveRate)
        spinnerTerm1 = findViewById(R.id.spinnerTerm1)
        edtInterestRateLoan = findViewById(R.id.edtInterestRateLoan)
        btnSaveRateLoan = findViewById(R.id.btnSaveRateLoan)


        setupSpinner()
        setupLoanSpinner()
        loadCurrentRate()
        loadCurrentLoanRate()
        initActions()
    }

    private fun initActions() {
        btnSaveRate.setOnClickListener {
            saveInterestRate()
        }
        btnSaveRateLoan.setOnClickListener {
            saveLoanInterestRate()
        }
    }

    private fun setupSpinner() {
        val terms = arrayOf("6 tháng", "1 năm", "2 năm", "3 năm")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, terms)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTerm.adapter = adapter
    }
    private fun setupLoanSpinner() {
        val terms = arrayOf("1 năm", "2 năm", "3 năm", "5 năm")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, terms)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTerm1.adapter = adapter
    }

    private fun loadCurrentRate() {
        db.collection("interest_rates")
            .document("saving")
            .get()
            .addOnSuccessListener { doc ->
                val term = spinnerTerm.selectedItem.toString()
                val rate = doc.getDouble(term)
                if (rate != null) {
                    edtInterestRate.setText(rate.toString())
                }
            }
    }
    private fun loadCurrentLoanRate() {
        db.collection("interest_rates")
            .document("loan")
            .get()
            .addOnSuccessListener { doc ->
                val term = spinnerTerm1.selectedItem.toString()
                val rate = doc.getDouble(term)
                if (rate != null) {
                    edtInterestRateLoan.setText(rate.toString())
                }
            }
    }


    private fun saveInterestRate() {
        val term = spinnerTerm.selectedItem.toString()
        val rate = edtInterestRate.text.toString().toDoubleOrNull()

        if (rate == null || rate <= 0) {
            Toast.makeText(this, "Lãi suất không hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }

        if (rate < 0.01 || rate > 0.2) {
            Toast.makeText(
                this,
                "Lãi suất phải nằm trong khoảng 1% – 20% / năm",
                Toast.LENGTH_SHORT
            ).show()
            return
        }


        val updateData = mapOf(
            term to rate,
            "updatedAt" to Timestamp.now()
        )

        db.collection("interest_rates")
            .document("saving")
            .set(updateData, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Cập nhật lãi suất thành công", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveLoanInterestRate() {
        val term = spinnerTerm1.selectedItem.toString()
        val rate = edtInterestRateLoan.text.toString().toDoubleOrNull()

        if (rate == null || rate <= 0) {
            Toast.makeText(this, "Lãi suất vay không hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }

        if (rate < 0.08 || rate > 0.15) {
            Toast.makeText(
                this,
                "Lãi suất vay phải trong khoảng 8% – 15% / năm",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val updateData = mapOf(
            term to rate,
            "updatedAt" to Timestamp.now()
        )

        db.collection("interest_rates")
            .document("loan")
            .set(updateData, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Cập nhật lãi suất vay thành công", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

}
