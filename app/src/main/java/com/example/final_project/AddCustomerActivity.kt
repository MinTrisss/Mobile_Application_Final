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
import android.view.Menu
import android.view.MenuItem
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
    private lateinit var edtPassword: EditText   // ➕ cho login
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
            Toast.makeText(this, "Giới tính không được để trống!", Toast.LENGTH_SHORT).show()
            return
        }
        val gender = if (genderId == R.id.radioMale) "Nam" else "Nữ"


        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty() ||
            nationalId.isEmpty() || dobInput.isEmpty() || address.isEmpty()
        ) {
            Toast.makeText(this, "Xin hãy điền đầy đủ thông tin!", Toast.LENGTH_SHORT).show()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Định dạng email không hợp lệ!", Toast.LENGTH_SHORT).show()
            return
        }

        if(phone.length != 10) {
            Toast.makeText(this, "Số điện thoại không đúng!", Toast.LENGTH_SHORT).show()
            return
        }

        if(nationalId.length != 12) {
            Toast.makeText(this, "CCCD/CMND không hợp lệ!", Toast.LENGTH_SHORT).show()
            return
        }
        if(password.length < 6) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự!", Toast.LENGTH_SHORT).show()
            return
        }

        // DATE → TIMESTAMP
        val dobTimestamp = parseDateToTimestamp(dobInput)
        if (dobTimestamp == null) {
            Toast.makeText(this, "Ngày sinh phải là dd/MM/yyyy!", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("DEBUG", "imageUri = $imageUri")


        // Create auth user
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener

                uploadAvatarAndSave(uid, name, email, phone, nationalId, dobTimestamp, gender, address)

            }
            .addOnFailureListener {
                Toast.makeText(this, "Tạo thất bại: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun parseDateToTimestamp(dateStr: String): Timestamp? {
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = sdf.parse(dateStr)
            Timestamp(date!!)
        } catch (e: Exception) {
            null
        }
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
                    "%02d/%02d/%04d",
                    selectedDay,
                    selectedMonth + 1,
                    selectedYear
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
        name: String,
        email: String,
        phone: String,
        nationalId: String,
        dob: Timestamp,
        gender: String,
        address: String
    ) {
        if (imageUri == null) {
            saveCustomer(uid, name, email, phone, nationalId, dob, gender, address, "")
            return
        }

        val storageRef = FirebaseStorage.getInstance()
            .reference.child("avatars/$uid.jpg") // ✅ dùng uid

        storageRef.putFile(imageUri!!)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    saveCustomer(
                        uid, name, email, phone,
                        nationalId, dob, gender, address, uri.toString()
                    )
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
                saveCustomer(uid, name, email, phone, nationalId, dob, gender, address, "")
            }
    }



    private fun saveCustomer(
        uid: String,
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
                Toast.makeText(this, "Tạo khách hàng thành công!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
