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
import java.text.SimpleDateFormat
import java.util.*

class LoanManagementActivity : AppCompatActivity() {

    private lateinit var rvLoans: RecyclerView
    private lateinit var fabRegisterLoan: FloatingActionButton
    private lateinit var loansAdapter: LoanAdapter
    private val loansList = mutableListOf<Loan>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var loansListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loan_management)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rvLoans = findViewById(R.id.rvLoans)
        fabRegisterLoan = findViewById(R.id.fabRegisterLoan)

        rvLoans.layoutManager = LinearLayoutManager(this)
        loansAdapter = LoanAdapter(loansList)
        rvLoans.adapter = loansAdapter

        fabRegisterLoan.setOnClickListener {
            startActivity(Intent(this, RegisterLoanActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        listenForLoanChanges()
    }

    override fun onStop() {
        super.onStop()
        loansListener?.remove()
    }

    private fun listenForLoanChanges() {
        val uid = auth.currentUser?.uid ?: return

        loansListener = db.collection("accounts")
            .whereEqualTo("uid", uid)
            .whereEqualTo("type", "loan")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->

                if (e != null || snapshots == null) {
                    Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                loansList.clear()

                for (doc in snapshots.documents) {

                    loansList.add(
                        Loan(
                            id = doc.id,
                            uid = uid,
                            principal = doc.getDouble("principal") ?: 0.0,

                            // ✅ LẤY TRỰC TIẾP TỪ DOCUMENT LOAN
                            interestRate = doc.getDouble("interestRate") ?: 0.0,

                            termMonths = (doc.getLong("termMonths") ?: 12L).toInt(),

                            monthlyPayment = doc.getDouble("monthlyPayment") ?: 0.0,

                            status = doc.getString("status") ?: "PENDING",

                            totalPayment = doc.getDouble("totalPayment") ?: 0.0,
                            totalInterest = doc.getDouble("totalInterest") ?: 0.0,

                            createdAt = doc.getTimestamp("createdAt")?.toDate() ?: Date()
                        )
                    )
                }

                loansAdapter.notifyDataSetChanged()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
