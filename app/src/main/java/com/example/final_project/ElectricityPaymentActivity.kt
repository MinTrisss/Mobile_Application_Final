package com.example.final_project

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

data class ElectricityBill(val customerCode: String, val amount: Double, val provider: String)

class ElectricityPaymentActivity : BasePaymentActivity() {

    private lateinit var edtElecCustomerCode: EditText
    private lateinit var btnCheckElecCustomer: Button
    private lateinit var edtElecAmount: EditText
    private lateinit var btnPayElectricity: Button

    private val mockBills = listOf(
        ElectricityBill("PE0123456789", 550000.0, "EVN HCMC"),
        ElectricityBill("PC9876543210", 1200000.0, "EVN Hanoi"),
        ElectricityBill("PD1122334455", 875000.0, "EVN Da Nang")
    )
    private var currentBill: ElectricityBill? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_electricity_payment)

        initViews()
        initActions()
    }

    private fun initViews() {
        edtElecCustomerCode = findViewById(R.id.edtElecCustomerCode)
        btnCheckElecCustomer = findViewById(R.id.btnCheckElecCustomer)
        edtElecAmount = findViewById(R.id.edtElecAmount)
        btnPayElectricity = findViewById(R.id.btnPayElectricity)
    }

    private fun initActions() {
        btnCheckElecCustomer.setOnClickListener { checkBill() }
        
        btnPayElectricity.setOnClickListener {
            currentBill?.let {
                // Truyền thêm amount vào intent để onActivityResult có thể lấy lại
                intent.putExtra("amount", it.amount)
                launchVnPayGateway(it.amount, "Thanh toan tien dien ${it.customerCode}")
            }
        }
    }

    private fun checkBill(){
        val customerCode = edtElecCustomerCode.text.toString()
        val bill = mockBills.find { it.customerCode.equals(customerCode, ignoreCase = true) }

        if (bill != null) {
            currentBill = bill
            edtElecAmount.setText(String.format("%,.0f VNĐ", bill.amount))
            btnPayElectricity.isEnabled = true
            Toast.makeText(this, "Đã tìm thấy hóa đơn của ${bill.provider}", Toast.LENGTH_SHORT).show()
        } else {
            currentBill = null
            edtElecAmount.setText("")
            btnPayElectricity.isEnabled = false
            Toast.makeText(this, "Không tìm thấy hóa đơn cho mã khách hàng này", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onPaymentSuccess(amount: Double) {
        currentBill?.let {
            val note = "Thanh toan tien dien ${it.customerCode}"
            performTransaction(it.amount, note, "electricity", "EVN")
        }
    }
}
