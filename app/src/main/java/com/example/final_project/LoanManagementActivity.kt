package com.example.final_project

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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

data class Loan(val id: String, val uid: String, val amount: Double, val duration: String, val status: String, val createdAt: Date)

class LoanManagementActivity : AppCompatActivity() {

    private lateinit var rvLoans: RecyclerView
    private lateinit var fabRegisterLoan: FloatingActionButton
    private lateinit var loansAdapter: LoanAdapter
    private val loansList = mutableListOf<Loan>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var loansListener: ListenerRegistration? = null

    companion object {
        private const val RC_REGISTER_LOAN = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loan_management)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rvLoans = findViewById(R.id.rvLoans)
        fabRegisterLoan = findViewById(R.id.fabRegisterLoan)

        setupRecyclerView()

        fabRegisterLoan.setOnClickListener {
            val intent = Intent(this, RegisterLoanActivity::class.java)
            startActivityForResult(intent, RC_REGISTER_LOAN)
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

        val query = db.collection("accounts")
            .whereEqualTo("uid", uid)
            .whereEqualTo("type", "loan")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        loansListener = query.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Toast.makeText(this, "Lỗi khi tải dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            loansList.clear()
            for (doc in snapshots!!) {
                val loan = Loan(
                    id = doc.id,
                    uid = doc.getString("uid") ?: "",
                    amount = doc.getDouble("loan") ?: 0.0,
                    duration = doc.getString("duration") ?: "N/A",
                    status = doc.getString("status") ?: "N/A",
                    createdAt = doc.getTimestamp("createdAt")?.toDate() ?: Date()
                )
                loansList.add(loan)
            }
            loansAdapter.notifyDataSetChanged()
        }
    }

    private fun setupRecyclerView() {
        loansAdapter = LoanAdapter(loansList)
        rvLoans.layoutManager = LinearLayoutManager(this)
        rvLoans.adapter = loansAdapter
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

class LoanAdapter(private val loans: List<Loan>) : RecyclerView.Adapter<LoanAdapter.LoanViewHolder>() {

    class LoanViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLoanId: TextView = view.findViewById(R.id.tvLoanId)
        val tvLoanStatus: TextView = view.findViewById(R.id.tvLoanStatus)
        val tvLoanAmount: TextView = view.findViewById(R.id.tvLoanAmount)
        val tvLoanDuration: TextView = view.findViewById(R.id.tvLoanDuration)
        val tvLoanDate: TextView = view.findViewById(R.id.tvLoanDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoanViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_loan, parent, false)
        return LoanViewHolder(view)
    }

    override fun onBindViewHolder(holder: LoanViewHolder, position: Int) {
        val loan = loans[position]
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        holder.tvLoanId.text = "Mã khoản vay: #${loan.uid}"
        holder.tvLoanStatus.text = loan.status
        holder.tvLoanAmount.text = "Số tiền vay: ${String.format("%,.0f", loan.amount)} VNĐ"
        holder.tvLoanDuration.text = "Kỳ hạn: ${loan.duration}"
        holder.tvLoanDate.text = "Ngày đăng ký: ${sdf.format(loan.createdAt)}"
    }

    override fun getItemCount() = loans.size
}
