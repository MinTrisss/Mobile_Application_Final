package com.example.final_project

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore


class EmployeeHomeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CustomerAdapter
    private val db = FirebaseFirestore.getInstance()
    private val customerList = ArrayList<Customer>()
    private val customerListOriginal = ArrayList<Customer>()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_employee_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnAddCustomer = findViewById<Button>(R.id.btnAddCustomer)
        val btnLogout = findViewById<ImageView>(R.id.btnLogoutEmployee)
        val txtOfficerTitle = findViewById<TextView>(R.id.txtOfficerTitle)
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid != null) {
            db.collection("employees")
                .document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    val name = doc.getString("name") ?: "khách hàng"
                    txtOfficerTitle.text = "Xin chào,\n$name"
                }
        }


        val spinnerSort = findViewById<Spinner>(R.id.spinnerSort)
        val sortOptions = arrayOf(
            "Tên A → Z",
            "Tên Z → A",
            "Trạng thái Z → A",
            "Trạng thái A → Z"
        )
        spinnerSort.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sortOptions)

        spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                when (position) {
                    0 -> customerList.sortBy { it.name.lowercase() }
                    1 -> customerList.sortByDescending { it.name.lowercase() }
                    2 -> customerList.sortBy { it.status.lowercase() }
                    3 -> customerList.sortByDescending { it.status.lowercase() }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val searchView = findViewById<SearchView>(R.id.searchView)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                val keyword = newText?.lowercase()?.trim() ?: ""

                customerList.clear()

                if (keyword.isEmpty()) {
                    customerList.addAll(customerListOriginal)
                } else {
                    val filtered = customerListOriginal.filter {
                        it.name.lowercase().contains(keyword) ||
                                it.uid.lowercase().contains(keyword) ||
                                it.role.lowercase().contains(keyword) ||
                                it.email.lowercase().contains(keyword) ||
                                it.status.lowercase().contains(keyword) ||
                                it.phoneNum.lowercase().contains(keyword)
                    }
                    customerList.addAll(filtered)
                }

                adapter.notifyDataSetChanged()
                return true
            }

        })

        btnAddCustomer.setOnClickListener {
            startActivity(Intent(this, AddCustomerActivity::class.java))
        }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        recyclerView = findViewById(R.id.recyclerCustomers)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = CustomerAdapter(
            list = customerList,
            onEdit = {customer ->
                val intent = Intent(this, EditCustomerActivity::class.java)
                intent.putExtra("uid", customer.uid)
                startActivity(intent)
            },
            onToggleStatus = { customer ->
                showDeleteConfirm(customer)
            },
            onItemClick = { customer ->
                val intent = Intent(this, CustomerDetailActivity::class.java)
                intent.putExtra("uid", customer.uid)
                startActivity(intent)
            }
        )

        recyclerView.adapter = adapter

        loadCustomersRealtime()
    }

    private fun loadCustomersRealtime() {
        db.collection("customers")
            .orderBy("name")
            .addSnapshotListener { snaps, err ->
                if (err != null) {
                    Toast.makeText(this, "Tải dữ liệu khách hàng thất bại!", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                customerList.clear()
                customerListOriginal.clear()
                snaps?.forEach { doc ->

                    val user = Customer(
                        uid = doc.id,
                        nationalId = doc.getString("nationalId") ?: "",
                        name = doc.getString("name") ?: "",
                        phoneNum = doc.getString("phoneNum") ?: "",
                        role = doc.getString("role") ?: "",
                        status = doc.getString("status") ?: "Hoạt động",
                        email = doc.getString("email") ?: "",
                        avtURL = doc.getString("imageURL") ?: ""
                    )

                    customerList.add(user)
                    customerListOriginal.add(user)
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun showDeleteConfirm(customer: Customer) {
        AlertDialog.Builder(this)
            .setTitle("Xoá khách hàng")
            .setMessage("Bạn có muốn xoá khách hàng \"${customer.name}\"?")
            .setPositiveButton("Xoá khách hàng") { _, _ -> toggleCustomerStatus(customer) }
            .setNegativeButton("Huỷ", null)
            .show()
    }
    private fun toggleCustomerStatus(customer: Customer) {
        val newStatus = if (customer.status == "Hoạt động") "Đã khoá" else "Hoạt động"

        db.collection("customers").document(customer.uid)
            .update("status", newStatus)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    if (newStatus == "Đã khoá") "Đã khoá"
                    else "Tài khoản đã được mở khoá",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

}
