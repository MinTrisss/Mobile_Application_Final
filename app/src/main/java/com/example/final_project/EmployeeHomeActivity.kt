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

        val spinnerSort = findViewById<Spinner>(R.id.spinnerSort)
        val sortOptions = arrayOf(
            "Name A → Z",
            "Name Z → A",
            "Customer ID A → Z",
            "Customer ID Z → A",
            "Status Z → A",
            "Status A → Z"
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
                    2 -> customerList.sortBy { it.customerId.lowercase() }
                    3 -> customerList.sortByDescending { it.customerId.lowercase() }
                    4 -> customerList.sortBy { it.status.lowercase() }
                    5 -> customerList.sortByDescending { it.status.lowercase() }
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

            // Quay về màn hình login
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        recyclerView = findViewById(R.id.recyclerCustomers)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = CustomerAdapter(
            list = customerList,
            onEdit = { user ->
//                val intent = Intent(this, EditUserActivity::class.java)
//                intent.putExtra("uid", user.uid)
//                startActivity(intent)
            },
            onToggleStatus = { customer ->
                showDeleteConfirm(customer)
            },
            onItemClick = { user ->
//                val intent = Intent(this, UserDetailActivity::class.java)
//                intent.putExtra("uid", user.uid)
//                startActivity(intent)
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
                    Toast.makeText(this, "Load customers failed", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                customerList.clear()
                customerListOriginal.clear()
                snaps?.forEach { doc ->

                    val user = Customer(
                        uid = doc.id,
                        customerId = doc.getString("customerId") ?: "",
                        name = doc.getString("name") ?: "",
                        phoneNum = doc.getString("phoneNum") ?: "",
                        role = doc.getString("role") ?: "",
                        status = doc.getString("status") ?: "normal",
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
            .setTitle("Delete customer")
            .setMessage("Are you sure to delete customer \"${customer.name}\"? This will remove customer's document from Firestore but will NOT remove Authentication account.")
            .setPositiveButton("Delete") { _, _ -> toggleCustomerStatus(customer) }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun toggleCustomerStatus(customer: Customer) {
        val newStatus = if (customer.status == "normal") "locked" else "normal"

        db.collection("customers").document(customer.uid)
            .update("status", newStatus)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    if (newStatus == "locked") "Customer locked"
                    else "Customer unlocked",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

}
