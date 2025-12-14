package com.example.final_project

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

// Đảm bảo các trường ID tài khoản là String
data class TransactionModel(
    val amount: Double = 0.0,
    val customerId: String = "",
    val fromAccountId: String = "",
    val note: String = "",
    val status: String = "",
    val timestamp: Date = Date(),
    val toAccountId: String = "",
    val transactionId: String = "",
    val type: String = ""
)

class TransactionHistoryActivity : AppCompatActivity() {

    private lateinit var rvTransactions: RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter
    private val transactionList = mutableListOf<TransactionModel>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var transactionListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_history)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rvTransactions = findViewById(R.id.rvTransactions)
        setupRecyclerView()
    }

    override fun onStart() {
        super.onStart()
        listenForTransactions()
    }

    override fun onStop() {
        super.onStop()
        transactionListener?.remove()
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(transactionList)
        rvTransactions.layoutManager = LinearLayoutManager(this)
        rvTransactions.adapter = transactionAdapter
    }

    private fun listenForTransactions() {
        val customerId = auth.currentUser?.uid ?: return

        val query = db.collection("transactions")
            .whereEqualTo("customerId", customerId)

        transactionListener = query.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Toast.makeText(this, "Lỗi tải lịch sử giao dịch: ${e.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            transactionList.clear()
            snapshots?.documents?.forEach { doc ->
                val transaction = doc.toObject(TransactionModel::class.java)
                if (transaction != null) {
                    transactionList.add(transaction)
                }
            }

            transactionList.sortByDescending { it.timestamp }
            
            transactionAdapter.notifyDataSetChanged()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

class TransactionAdapter(private val transactions: List<TransactionModel>) 
    : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.ivTransactionIcon)
        val type: TextView = view.findViewById(R.id.tvTransactionType)
        val note: TextView = view.findViewById(R.id.tvTransactionNote)
        val date: TextView = view.findViewById(R.id.tvTransactionDate)
        val amount: TextView = view.findViewById(R.id.tvTransactionAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        holder.date.text = sdf.format(transaction.timestamp)
        holder.note.text = if(transaction.note.isNotBlank()) transaction.note else "N/A"
        
        holder.amount.text = "- ${String.format("%,.0f", transaction.amount)} VNĐ"
        holder.amount.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.holo_red_dark))

        when(transaction.type) {
            "internal_transfer" -> {
                holder.type.text = "Chuyển đến TK ${transaction.toAccountId}"
                holder.icon.setImageResource(R.drawable.ic_deposit)
            }
            "electricity" -> {
                holder.type.text = "Thanh toán tiền điện"
                holder.icon.setImageResource(R.drawable.ic_electricity)
            }
            "water" -> {
                holder.type.text = "Thanh toán tiền nước"
                holder.icon.setImageResource(R.drawable.ic_water)
            }
            "movie_ticket" -> {
                holder.type.text = "Thanh toán vé xem phim"
                holder.icon.setImageResource(R.drawable.ic_ticket)
            }
            else -> {
                holder.type.text = "Nạp tiền điện thoại"
                holder.icon.setImageResource(R.drawable.ic_phone)
            }
        }
    }

    override fun getItemCount() = transactions.size
}
