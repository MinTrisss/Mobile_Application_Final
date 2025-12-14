package com.example.final_project

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

class PhoneTopUpActivity : BasePaymentActivity() {

    private lateinit var tvTopUpTitle: TextView
    private lateinit var edtPhoneNumber: EditText
    private lateinit var spinnerAmounts: Spinner
    private lateinit var btnTopUp: Button

    private val amounts = arrayOf(10000.0, 20000.0, 50000.0, 100000.0, 200000.0, 500000.0)
    private var selectedProvider: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_top_up)

        selectedProvider = intent.getStringExtra("providerName")

        initViews()
        setupSpinner()
        initActions()
    }

    private fun initViews() {
        tvTopUpTitle = findViewById(R.id.tvTopUpTitle)
        edtPhoneNumber = findViewById(R.id.edtPhoneNumber)
        spinnerAmounts = findViewById(R.id.spinnerAmounts)
        btnTopUp = findViewById(R.id.btnTopUp)

        // Cập nhật tiêu đề với tên nhà mạng
        tvTopUpTitle.text = "Nạp tiền - $selectedProvider"
    }

    private fun setupSpinner() {
        val amountStrings = amounts.map { String.format("%,.0f VNĐ", it) }
        val amountAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, amountStrings)
        amountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAmounts.adapter = amountAdapter
    }

    private fun initActions() {
        btnTopUp.setOnClickListener {
            val phoneNumber = edtPhoneNumber.text.toString()
            if (phoneNumber.length < 9) {
                Toast.makeText(this, "Số điện thoại không hợp lệ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedAmount = amounts[spinnerAmounts.selectedItemPosition]
            val orderInfo = "Nap tien cho so $phoneNumber"

            // Lưu thông tin vào intent để onPaymentSuccess có thể lấy lại
            intent.putExtra("amount", selectedAmount)
            intent.putExtra("note", "Nap tien $selectedAmount cho $phoneNumber ($selectedProvider)")
            intent.putExtra("toAccountId", selectedProvider)

            launchVnPayGateway(selectedAmount, orderInfo)
        }
    }

    // SỬA LỖI: Ghi đè hàm abstract onPaymentSuccess từ lớp cha
    override fun onPaymentSuccess(amount: Double) {
        val note = intent.getStringExtra("note") ?: ""
        val toAccountId = intent.getStringExtra("toAccountId") ?: ""
        
        performTransaction(amount, note, "phone_topup", toAccountId)
    }
}
