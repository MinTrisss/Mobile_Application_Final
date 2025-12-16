package com.example.final_project

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Locale

class CustomerHomeActivity : AppCompatActivity() {
    private lateinit var txtWelcome: TextView
    private lateinit var txtBalance: TextView
    private lateinit var txtAccountNumber: TextView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var customerListener: ListenerRegistration? = null
    private var accountListener: ListenerRegistration? = null




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_customer_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        initActions()
    }

    override fun onStart() {
        super.onStart()
        setupRealtimeListeners()
    }

    override fun onStop() {
        super.onStop()
        customerListener?.remove()
        accountListener?.remove()
    }

    private fun initViews() {
        txtWelcome = findViewById(R.id.txtWelcome)
        txtBalance = findViewById(R.id.txtBalance)
        txtAccountNumber = findViewById(R.id.txtAccountNumber)
    }

    private fun setupRealtimeListeners() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        val uid = currentUser.uid

        customerListener = db.collection("customers").document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) { return@addSnapshotListener }
                if (snapshot != null && snapshot.exists()) {
                    txtWelcome.text = "Xin chào, ${snapshot.getString("name")}"
                }
            }

        accountListener = db.collection("accounts")
            .whereEqualTo("uid", uid)
            .whereEqualTo("type", "checking")
            .limit(1)
            .addSnapshotListener { snapshots, e ->
                if (e != null) { return@addSnapshotListener }
                if (snapshots != null && !snapshots.isEmpty) {
                    val doc = snapshots.documents[0]
                    val balance = doc.getDouble("balance") ?: 0.0
                    val accountId = doc.getString("accountId") ?: ""
                    txtBalance.text = String.format("%,.0f VNĐ", balance)
                    txtAccountNumber.text = "Số tài khoản: $accountId"
                } else {
                    txtBalance.text = "0 VNĐ"
                    txtAccountNumber.text = "Số tài khoản: N/A"
                }
            }
    }

    private fun initActions() {
        val btnLogOut = findViewById<ImageView>(R.id.btnLogoutCustomer)
        val btnInternalTransfer = findViewById<View>(R.id.btnInternalTransfer)
        val btnTransactionHistory = findViewById<View>(R.id.btnTransactionHistory)
        val btnAccountManagement = findViewById<View>(R.id.btnAccountManagement)
        val btnUtilitiesElectric = findViewById<View>(R.id.btnUtilitiesElectric)
        val btnUtilitiesWater = findViewById<View>(R.id.btnUtilitiesWater)


        btnLogOut.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        btnInternalTransfer.setOnClickListener {
             startActivity(Intent(this, TransferActivity::class.java))
        }

        btnTransactionHistory.setOnClickListener {
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
        }

        btnAccountManagement.setOnClickListener {
            startActivity(Intent(this, AccountManagementActivity::class.java))
        }
        btnUtilitiesElectric.setOnClickListener {
            startActivity(Intent(this, ElectricityPaymentActivity::class.java))
        }
        btnUtilitiesWater.setOnClickListener {
            startActivity(Intent(this, WaterPaymentActivity::class.java))
        }
    }
}
