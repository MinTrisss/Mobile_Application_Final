package com.example.final_project

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AccountManagementActivity : AppCompatActivity() {

    // --- UI Components ---
    // private lateinit var btnBack: ImageView // Đã xóa
    private lateinit var tvCheckingAccountNumber: TextView
    private lateinit var tvAccountHolder: TextView
    private lateinit var tvOpenDate: TextView
    private lateinit var tvCheckingBalance: TextView

    private lateinit var layoutSavingDetails: LinearLayout
    private lateinit var tvSavingAccountNumber: TextView
    private lateinit var tvSavingTerm: TextView
    private lateinit var tvSavingBalance: TextView
    private lateinit var tvSavingAccountStatus: TextView
    private lateinit var btnOpenSaving: Button
    private lateinit var btnSettleSaving: Button
    private lateinit var btnViewLoanDetails: Button

    // --- Firebase ---
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var savingAccountDocId: String? = null

    companion object {
        private const val RC_OPEN_SAVING = 1001
        private const val RC_REGISTER_LOAN = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_management)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()
        loadData()
        initActions()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_OPEN_SAVING && resultCode == Activity.RESULT_OK) {
            loadData()
        }
    }

    private fun initViews() {
        tvCheckingAccountNumber = findViewById(R.id.tvCheckingAccountNumber)
        tvAccountHolder = findViewById(R.id.tvAccountHolder)
        tvOpenDate = findViewById(R.id.tvOpenDate)
        tvCheckingBalance = findViewById(R.id.tvCheckingBalance)

        layoutSavingDetails = findViewById(R.id.layoutSavingDetails)
        tvSavingAccountNumber = findViewById(R.id.tvSavingAccountNumber)
        tvSavingTerm = findViewById(R.id.tvSavingTerm)
        tvSavingBalance = findViewById(R.id.tvSavingBalance)
        tvSavingAccountStatus = findViewById(R.id.tvSavingAccountStatus)
        btnOpenSaving = findViewById(R.id.btnOpenSaving)
        btnSettleSaving = findViewById(R.id.btnSettleSaving)
        
        btnViewLoanDetails = findViewById(R.id.btnViewLoanDetails)
    }

    private fun loadData() {
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid

        db.collection("customers").document(uid).get().addOnSuccessListener { doc ->
            if(doc.exists()){
                tvAccountHolder.text = "Chủ tài khoản: ${doc.getString("name")}"
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                tvOpenDate.text = "Ngày mở: ${doc.getTimestamp("createdAt")?.toDate()?.let { sdf.format(it) }}"
            }
        }
        
        db.collection("accounts").whereEqualTo("customerId", uid).get()
            .addOnSuccessListener { accountsSnapshot ->
                var foundSaving = false

                for (document in accountsSnapshot) {
                    val accountId = document.getLong("accountId")?.toString() ?: "N/A"
                    when (document.getString("type")) {
                        "checking" -> {
                             tvCheckingAccountNumber.text = "Số tài khoản: $accountId"
                             val balance = document.getDouble("balance") ?: 0.0
                             tvCheckingBalance.text = String.format("%,.0f VNĐ", balance)
                        }
                        "saving" -> {
                            foundSaving = true
                            savingAccountDocId = document.id
                            tvSavingAccountNumber.text = "Số tài khoản: $accountId"
                            tvSavingTerm.text = "Kỳ hạn: ${document.getString("Term")}"
                            tvSavingBalance.text = String.format("%,.0f VNĐ", document.getDouble("saving"))

                            layoutSavingDetails.visibility = View.VISIBLE
                            tvSavingAccountStatus.visibility = View.GONE
                            btnOpenSaving.visibility = View.GONE
                            btnSettleSaving.visibility = View.VISIBLE
                        }
                    }
                }

                if (!foundSaving) {
                    layoutSavingDetails.visibility = View.GONE
                    tvSavingAccountStatus.visibility = View.VISIBLE
                    btnOpenSaving.visibility = View.VISIBLE
                    btnSettleSaving.visibility = View.GONE
                }
            }
    }

    private fun initActions() {
        // btnBack.setOnClickListener { finish() } // Đã xóa

        btnOpenSaving.setOnClickListener {
            startActivityForResult(Intent(this, OpenSavingAccountActivity::class.java), RC_OPEN_SAVING)
        }

        btnSettleSaving.setOnClickListener {
            showSettleConfirmationDialog()
        }

        btnViewLoanDetails.setOnClickListener {
            startActivity(Intent(this, LoanManagementActivity::class.java))
        }
    }

    private fun showSettleConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Xác nhận tất toán")
            .setMessage("Bạn có chắc chắn muốn tất toán sớm tài khoản tiết kiệm không? Mọi lãi suất dự kiến sẽ bị mất.")
            .setPositiveButton("Đồng ý") { _, _ ->
                performSettleTransaction()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun performSettleTransaction() {
        val customerId = auth.currentUser?.uid
        val savingDocId = savingAccountDocId
        if (customerId == null || savingDocId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin tài khoản.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("accounts")
            .whereEqualTo("customerId", customerId)
            .whereEqualTo("type", "checking")
            .limit(1)
            .get()
            .addOnSuccessListener { checkingAccountSnapshot ->
                if (checkingAccountSnapshot.isEmpty) {
                    Toast.makeText(this, "Lỗi: Không tìm thấy tài khoản thanh toán để hoàn tiền.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                val checkingAccountRef = checkingAccountSnapshot.documents[0].reference
                val savingAccountRef = db.collection("accounts").document(savingDocId)

                db.runTransaction { transaction ->
                    val checkingSnap = transaction.get(checkingAccountRef)
                    val savingSnap = transaction.get(savingAccountRef)

                    if (!savingSnap.exists()) {
                        throw Exception("Tài khoản tiết kiệm không còn tồn tại.")
                    }

                    val currentCheckingBalance = checkingSnap.getDouble("balance") ?: 0.0
                    val savingAmount = savingSnap.getDouble("saving") ?: 0.0

                    val newCheckingBalance = currentCheckingBalance + savingAmount
                    transaction.update(checkingAccountRef, "balance", newCheckingBalance)
                    transaction.delete(savingAccountRef)
                    null
                }.addOnSuccessListener {
                    Toast.makeText(this, "Tất toán thành công!", Toast.LENGTH_LONG).show()
                    loadData()
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Tất toán thất bại: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                 Toast.makeText(this, "Lỗi khi tìm tài khoản thanh toán: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
