package com.example.final_project

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
//import android.util.Log
//import android.view.Menu
//import android.view.MenuItem
import android.util.Log
import android.app.DatePickerDialog
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class AddCustomerActivity : AppCompatActivity() {

    private lateinit var edtFullName: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtPhone: EditText
    private lateinit var edtNationalId: EditText
    private lateinit var edtDOB: EditText
    private lateinit var edtPassword: EditText
    private lateinit var edtAddress: EditText
    private lateinit var radioGroupGender: RadioGroup
    private lateinit var btnAddCustomer: Button
    private lateinit var btnChooseImage: Button
    private lateinit var imgAvatarPreview: ImageView


    private var imageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1001

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_customer)

        val toolbar = findViewById<Toolbar>(R.id.toolbarAddCustomer)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Add Customer"
            setDisplayHomeAsUpEnabled(true)
        }
        // Mapping views
        edtFullName = findViewById(R.id.edtFullName)
        edtEmail = findViewById(R.id.edtEmail)
        edtPhone = findViewById(R.id.edtPhone)
        edtNationalId = findViewById(R.id.edtNationalId)
        edtDOB = findViewById(R.id.edtDOB)
        edtPassword = findViewById(R.id.edtPassword)
        edtAddress = findViewById(R.id.edtAddress)
        radioGroupGender = findViewById(R.id.radioGender)
        btnAddCustomer = findViewById(R.id.btnAddCustomer)
        btnChooseImage = findViewById(R.id.btnChooseImage)
        imgAvatarPreview = findViewById(R.id.imgAvatarPreview)

        // Sau đó mới được set sự kiện
        edtDOB.setOnClickListener { showDatePicker() }
        btnChooseImage.setOnClickListener { pickImageFromGallery() }
        btnAddCustomer.setOnClickListener { addCustomer() }

    }

    // Pick avatar
    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            imgAvatarPreview.setImageURI(imageUri)
        }
    }

    private fun addCustomer() {
        val name = edtFullName.text.toString().trim()
        val email = edtEmail.text.toString().trim()
        val phone = edtPhone.text.toString().trim()
        val nationalId = edtNationalId.text.toString().trim()
        val dobInput = edtDOB.text.toString().trim()  // yyyy-MM-dd
        val address = edtAddress.text.toString().trim()
        val password = edtPassword.text.toString().trim()

        val genderId = radioGroupGender.checkedRadioButtonId
        if (genderId == -1) {
            Toast.makeText(this, "Please choose gender!", Toast.LENGTH_SHORT).show()
            return
        }
        val gender = if (genderId == R.id.radioMale) "male" else "female"


        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty() ||
            nationalId.isEmpty() || dobInput.isEmpty() || address.isEmpty()
        ) {
            Toast.makeText(this, "Please fill all information!", Toast.LENGTH_SHORT).show()
            return
        }

        // DATE → TIMESTAMP
        val dobTimestamp = parseDateToTimestamp(dobInput)
        if (dobTimestamp == null) {
            Toast.makeText(this, "DOB must be yyyy-MM-dd!", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("DEBUG", "imageUri = $imageUri")


        // Create auth user
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener
                generateCustomerId { customerId ->
                    uploadAvatarAndSave(uid, customerId, name, email, phone, nationalId, dobTimestamp, gender, address)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Create auth failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Convert yyyy-MM-dd → Timestamp
    private fun parseDateToTimestamp(dateStr: String): Timestamp? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateStr)
            Timestamp(date!!)
        } catch (e: Exception) {
            null
        }
    }

    private fun generateCustomerId(callback: (String) -> Unit) {
        val counterRef = db.collection("counters").document("customers")

        db.runTransaction { t ->
            val snap = t.get(counterRef)
            val current = snap.getLong("currentNumber") ?: 0
            val next = current + 1

            t.update(counterRef, "currentNumber", next)
            "CUS%04d".format(next)
        }.addOnSuccessListener { callback(it) }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val formatted = String.format(
                    "%04d-%02d-%02d",
                    selectedYear,
                    selectedMonth + 1,
                    selectedDay
                )
                edtDOB.setText(formatted)
            },
            year, month, day
        )

        datePicker.show()
    }

    private fun getRealUri(uri: Uri): Uri {
        if (uri.toString().contains("com.google.android.apps.photos.contentprovider"))
            return Uri.parse(uri.toString().replace("/ORIGINAL", ""))

        return uri
    }

    private fun uploadAvatarAndSave(
        uid: String,
        customerId: String,
        name: String,
        email: String,
        phone: String,
        nationalId: String,
        dob: Timestamp,
        gender: String,
        address: String,
    ) {
        if (imageUri == null) {
            saveCustomer(uid, customerId, name, email, phone, nationalId, dob, gender, address, "")
            return
        }

        val storageRef = FirebaseStorage.getInstance()
            .reference.child("avatars/$customerId.jpg")

        storageRef.putFile(imageUri!!)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    saveCustomer(
                        uid, customerId, name, email, phone,
                        nationalId, dob, gender, address, uri.toString(),
                    )
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
                saveCustomer(uid, customerId, name, email, phone, nationalId, dob, gender, address, "")
            }
    }


    private fun saveCustomer(
        uid: String,
        customerId: String,
        name: String,
        email: String,
        phone: String,
        nationalId: String,
        dob: Timestamp,
        gender: String,
        address: String,
        avatarUrl: String
    ) {
        val data = mapOf(
            "uid" to uid,
            "customerId" to customerId,
            "name" to name,
            "email" to email,
            "phoneNum" to phone,
            "nationalId" to nationalId,
            "dateOfBirth" to dob,
            "gender" to gender,
            "address" to address,
            "avtURL" to avatarUrl,
            "ekycStatus" to "pending",
            "status" to "normal",
            "role" to "customer",
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("customers").document(uid)
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Customer created successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
