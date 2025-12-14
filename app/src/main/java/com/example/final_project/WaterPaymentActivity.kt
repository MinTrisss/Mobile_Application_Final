package com.example.final_project

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

data class WaterBill(val customerCode: String, val amount: Double, val provider: String)

class WaterPaymentActivity : BasePaymentActivity() {

    private lateinit var edtWaterCustomerCode: EditText
    private lateinit var btnCheckWaterCustomer: Button
    private lateinit var edtWaterAmount: EditText
    private lateinit var btnPayWater: Button

    private val mockBills = listOf(
        WaterBill("W01234567", 250000.0, "Sawa HCMC"),
        WaterBill("W98765432", 320000.0, "Hanoi Water"),
        WaterBill("WD5558889", 180000.0, "Da Nang Water")
    )
    private var currentBill: WaterBill? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_water_payment)

        initViews()
        initActions()
    }

    private fun initViews() {
        edtWaterCustomerCode = findViewById(R.id.edtWaterCustomerCode)
        btnCheckWaterCustomer = findViewById(R.id.btnCheckWaterCustomer)
        edtWaterAmount = findViewById(R.id.edtWaterAmount)
        btnPayWater = findViewById(R.id.btnPayWater)
    }

    private fun initActions() {
        btnCheckWaterCustomer.setOnClickListener { checkBill() }

        btnPayWater.setOnClickListener {
            currentBill?.let {
                intent.putExtra("amount", it.amount)
                launchVnPayGateway(it.amount, "Thanh toan tien nuoc ${it.customerCode}")
            }
        }
    }

    private fun checkBill() {
        val customerCode = edtWaterCustomerCode.text.toString()
        val bill = mockBills.find { it.customerCode.equals(customerCode, ignoreCase = true) }

        if (bill != null) {
            currentBill = bill
            edtWaterAmount.setText(String.format("%,.0f VNĐ", bill.amount))
            btnPayWater.isEnabled = true
            Toast.makeText(this, "Đã tìm thấy hóa đơn của ${bill.provider}", Toast.LENGTH_SHORT).show()
        } else {
            currentBill = null
            edtWaterAmount.setText("")
            btnPayWater.isEnabled = false
            Toast.makeText(this, "Không tìm thấy hóa đơn cho mã khách hàng này", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPaymentSuccess(amount: Double) {
        currentBill?.let {
            val note = "Thanh toan tien nuoc ${it.customerCode}"
            performTransaction(it.amount, note, "water", "SAWACO")
        }
    }
}
