package com.example.final_project

import android.app.Activity
import android.content.Intent
import android.os.Bundle

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import android.util.Log
import androidx.appcompat.app.AlertDialog
import java.util.*

class LoanManagementActivity : AppCompatActivity() {

    private lateinit var rvLoans: RecyclerView
    private lateinit var fabRegisterLoan: FloatingActionButton
    private lateinit var loansAdapter: LoanAdapter
    private val loansList = mutableListOf<Loan>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var loansListener: ListenerRegistration? = null
    private var userRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loan_management)

        userRole = intent.getStringExtra("userRole")

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rvLoans = findViewById(R.id.rvLoans)
        fabRegisterLoan = findViewById(R.id.fabRegisterLoan)

        rvLoans.layoutManager = LinearLayoutManager(this)
        loansAdapter = LoanAdapter(loansList, isEmployee = userRole == "employee", onStatusClick = { loan ->
            showStatusUpdateDialog(loan)
        })
        rvLoans.adapter = loansAdapter

        fabRegisterLoan.setOnClickListener {
            startActivity(Intent(this, RegisterLoanActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        listenForLoanChanges()
    }

    private fun listenForLoanChanges() {
        val uid = auth.currentUser?.uid ?: return

        Log.d("LOAN_DEBUG", "Current User Role: $userRole")

        var baseQuery = db.collection("accounts").whereEqualTo("type", "loan")

        if (userRole != "employee") {
            baseQuery = baseQuery.whereEqualTo("uid", uid)
        }

        // Lắng nghe realtime
        loansListener = baseQuery
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener(this) { snapshots, e ->
                if (e != null) {
                    Log.e("LOAN_ERROR", "Error: ${e.message}")
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    loansList.clear()
                    for (doc in snapshots.documents) {
                        loansList.add(mapDocumentToLoan(doc))
                    }
                    loansAdapter.notifyDataSetChanged()
                    Log.d("LOAN_DEBUG", "Realtime update fired")
                }
            }
    }

    private fun mapDocumentToLoan(doc: com.google.firebase.firestore.DocumentSnapshot): Loan {
        return Loan(
            id = doc.id,
            uid = doc.getString("uid") ?: "",
            principal = doc.getDouble("principal") ?: 0.0,
            interestRate = doc.getDouble("interestRate") ?: 0.0,
            termMonths = (doc.getLong("termMonths") ?: 12L).toInt(),
            monthlyPayment = doc.getDouble("monthlyPayment") ?: 0.0,
            status = doc.getString("status") ?: "Chờ duyệt",
            totalPayment = doc.getDouble("totalPayment") ?: 0.0,
            totalInterest = doc.getDouble("totalInterest") ?: 0.0,
            createdAt = doc.getTimestamp("createdAt")?.toDate() ?: Date()
        )
    }

    // Thêm vào trong LoanManagementActivity
    private fun showStatusUpdateDialog(loan: Loan) {
        val statuses: Array<String>

        if (loan.status == "Chấp nhận" || loan.status == "Từ chối") {
            statuses = arrayOf("Chấp nhận", "Từ chối")
        } else {
            statuses = arrayOf("Chấp nhận", "Chờ duyệt", "Từ chối")
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Thay đổi trạng thái cho #${loan.id.takeLast(6)}")
        builder.setItems(statuses) { _, which ->
            val newStatus = statuses[which]
            updateLoanStatus(loan.id, newStatus)
        }
        builder.show()
    }

    private fun updateLoanStatus(loanId: String, newStatus: String) {
        val index = loansList.indexOfFirst { it.id == loanId }
        if (index != -1) {
            loansList[index] = loansList[index].copy(status = newStatus)
            loansAdapter.notifyItemChanged(index)
        }

        db.collection("accounts").document(loanId)
            .update("status", newStatus)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
