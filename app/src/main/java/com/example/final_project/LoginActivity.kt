package com.example.final_project

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent
import android.util.Log



class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnLogin: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_page)

        Log.d("LOGIN", "LoginActivity loaded")

        auth = FirebaseAuth.getInstance()
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        btnLogin = findViewById(R.id.btnLogin)

        val db = FirebaseFirestore.getInstance()

        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()


            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email and password must not be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val currentUser = auth.currentUser
                        val uid = currentUser?.uid ?: return@addOnCompleteListener
                        val emailLogged = currentUser.email ?: email

                        val logData = hashMapOf(
                            "uid" to uid,
                            "email" to emailLogged,
                            "loginTime" to com.google.firebase.Timestamp.now()
                        )

                        db.collection("loginHistory")
                            .add(logData)
                            .addOnSuccessListener {
                                Log.d("LOGIN_HISTORY", "Login logged successfully")
                            }
                            .addOnFailureListener { e ->
                                Log.e("LOGIN_HISTORY", "Failed to log login", e)
                            }

                        checkUserRoleAndNavigate()
                    } else {
                        Toast.makeText(
                            this,
                            task.exception?.message ?: "Authentication failed",
                            Toast.LENGTH_SHORT
                        ).show()

                    }
                }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun checkUserRoleAndNavigate() {
        val uid = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        // Step 1: Check Employee collection
        db.collection("employees").document(uid).get()
            .addOnSuccessListener { employeeDoc ->

                if (employeeDoc.exists()) {
                    // OPTIONAL: check locked
                    val status = employeeDoc.getString("status") ?: "normal"
                    if (status == "locked") {
                        auth.signOut()
                        Toast.makeText(this, "Employee account is locked.", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }

                    // Navigate to employee home
                    navigateToEmployeeScreen()
                    return@addOnSuccessListener
                }

                // Step 2: Not employee → Check Customer collection
                db.collection("customers").document(uid).get()
                    .addOnSuccessListener { customerDoc ->

                        if (customerDoc.exists()) {

                            val status = customerDoc.getString("status") ?: "normal"
                            if (status == "locked") {
                                auth.signOut()
                                Toast.makeText(this, "Customer account is locked.", Toast.LENGTH_LONG).show()
                                return@addOnSuccessListener
                            }

                            navigateToCustomerScreen()
                        } else {
                            // Not found anywhere
                            auth.signOut()
                            Toast.makeText(this, "User data not found!", Toast.LENGTH_LONG).show()
                        }
                    }
                    .addOnFailureListener {
                        auth.signOut()
                        Toast.makeText(this, "Cannot load customer info.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                auth.signOut()
                Toast.makeText(this, "Cannot load employee info.", Toast.LENGTH_SHORT).show()
            }
    }


    private fun navigateToEmployeeScreen() {
        Toast.makeText(applicationContext, "Successfully logged in to employee screen", Toast.LENGTH_LONG).show()
        val intent = Intent(this, EmployeeHomeActivity::class.java)
        intent.putExtra("userRole", "employee")
        startActivity(intent)
        finish()
    }

    private fun navigateToCustomerScreen() {
        Toast.makeText(applicationContext, "Successfully logged in to customer screen", Toast.LENGTH_LONG).show()
        val intent = Intent(this, CustomerHomeActivity::class.java)
        intent.putExtra("userRole", "admin")
        startActivity(intent)
        finish()
    }

}
