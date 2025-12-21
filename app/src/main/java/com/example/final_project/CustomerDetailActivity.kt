package com.example.final_project

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.ImageView
import android.widget.TextView
import android.content.Intent
import android.view.View
import android.widget.Button
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import android.util.Log
import java.util.*
class CustomerDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_customer_detail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbarCustomerDetail)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Thông tin khách hàng"
            setDisplayHomeAsUpEnabled(true)
        }

        val uid = intent.getStringExtra("uid")
            ?: run {
                finish()
                return
            }

        FirebaseFirestore.getInstance()
            .collection("customers")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                val customer = doc.toObject(Customer::class.java) ?: return@addOnSuccessListener

                bindCustomer(customer)
            }
    }

    private fun bindCustomer(customer: Customer) {
        Glide.with(this)
            .load(customer.avtURL)
            .placeholder(R.drawable.ic_user)
            .into(findViewById(R.id.imgAvatarDetail))

        findViewById<TextView>(R.id.txtCustomerName).text = "Họ và tên: ${customer.name}"
        findViewById<TextView>(R.id.txtNationalId).text = "CCCD/CMND: ${customer.nationalId}"
        findViewById<TextView>(R.id.txtPhoneDetail).text = "Số điện thoại: ${customer.phoneNum}"
        findViewById<TextView>(R.id.txtEmailDetail).text = "Email: ${customer.email}"
        findViewById<TextView>(R.id.txtAddressDetail).text = "Địa chỉ: ${customer.address}"
        findViewById<TextView>(R.id.txtStatusDetail).text = "Trạng thái: ${customer.status}"
        findViewById<TextView>(R.id.txtDOB).text =
            "Ngày sinh: ${formatTimestamp(customer.dateOfBirth)}"
        findViewById<TextView>(R.id.txtCreatedAtDetail).text =
            "Tạo vào: ${formatTimestamp(customer.createdAt)}"
        findViewById<TextView>(R.id.txtGender).text = "Giới tính: ${customer.gender}"

        val btnEdit = findViewById<Button>(R.id.btnEditCustomer)

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUid != null) {
            FirebaseFirestore.getInstance()
                .collection("employees")
                .document(currentUid)
                .get()
                .addOnSuccessListener { userDoc ->
                    val role = userDoc.getString("role")
                    Log.d("CustomerDetailActivity", "Role: $role")

                    if (role == "employee") {
                        btnEdit.visibility = View.GONE
                    }
                }
        }

        btnEdit.setOnClickListener {
            val intent = Intent(this, EditCustomerActivity::class.java)
            intent.putExtra("uid", customer.uid)
            startActivity(intent)
        }


        val avtURL = findViewById<ImageView>(R.id.imgAvatarDetail)
        Glide.with(this).load(customer.avtURL).into(avtURL)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_add_customer, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun formatTimestamp(ts: Timestamp?): String {
        if (ts == null) return "N/A"

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(ts.toDate())
    }
}