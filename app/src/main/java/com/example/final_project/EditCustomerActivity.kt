package com.example.final_project

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.bumptech.glide.Glide
import android.net.Uri
import android.widget.ImageView
import android.content.Intent
import android.app.Activity



class EditCustomerActivity : AppCompatActivity() {

    private lateinit var edtFullName: EditText
    private lateinit var edtPhone: EditText
    private lateinit var edtAddress: EditText
    private lateinit var edtDOB: EditText
    private lateinit var edtEKYC: EditText
    private lateinit var radioGender: RadioGroup
    private lateinit var edtNationalId: EditText
    private lateinit var edtEmail: EditText
    private lateinit var btnUpdate: Button
    private lateinit var btnChooseImage: Button
    private lateinit var imgAvatarPreview: ImageView
    private var imageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1001


    private val db = FirebaseFirestore.getInstance()

    private lateinit var uid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_customer)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbarEditCustomer)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Edit Student"
            setDisplayHomeAsUpEnabled(true)
        }

        edtFullName = findViewById(R.id.edtFullName)
        edtEmail = findViewById(R.id.edtEmail)
        edtNationalId = findViewById(R.id.edtNationalId)
        edtPhone = findViewById(R.id.edtPhone)
        edtAddress = findViewById(R.id.edtAddress)
        edtDOB = findViewById(R.id.edtDOB)
        edtEKYC = findViewById(R.id.edtEKYC)
        radioGender = findViewById(R.id.radioGender)
        btnUpdate = findViewById(R.id.btnUpdateCustomer)

        btnUpdate.setOnClickListener {
            updateCustomer()
        }

        edtDOB.setOnClickListener {
            showDatePicker()
        }

        btnChooseImage = findViewById(R.id.btnChooseImage)
        imgAvatarPreview = findViewById(R.id.imgAvatarPreview)
        btnChooseImage.setOnClickListener {
            pickImageFromGallery()
        }

        uid = intent.getStringExtra("uid") ?: ""
        if (uid.isNotEmpty()) {
            loadCustomerData()
        }
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

    private fun updateCustomer() {
        val name = edtFullName.text.toString().trim()
        val phone = edtPhone.text.toString().trim()
        val address = edtAddress.text.toString().trim()
        val ekyc = edtEKYC.text.toString().trim()
        val gender = if (radioGender.checkedRadioButtonId == R.id.radioMale) "Nam" else "Nữ"

        val dobString = edtDOB.text.toString()
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dobTimestamp = try {
            Timestamp(sdf.parse(dobString)!!)
        } catch (e: Exception) {
            null
        }

        btnUpdate.isEnabled = false // Khóa nút để tránh bấm nhiều lần

        if (imageUri != null) {
            // TRƯỜNG HỢP 1: CÓ CHỌN ẢNH MỚI
            val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance()
                .reference.child("avatars/$uid.jpg")

            storageRef.putFile(imageUri!!)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        saveToFirestore(name, phone, address, ekyc, gender, dobTimestamp, uri.toString())
                    }
                }
                .addOnFailureListener {
                    btnUpdate.isEnabled = true
                    Toast.makeText(this, "Lỗi tải ảnh: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // TRƯỜNG HỢP 2: KHÔNG ĐỔI ẢNH
            saveToFirestore(name, phone, address, ekyc, gender, dobTimestamp, null)
        }
    }

    private fun saveToFirestore(name: String, phone: String, address: String, ekyc: String, gender: String, dob: Timestamp?, avtURL: String?) {
        val data: MutableMap<String, Any> = hashMapOf(
            "name" to name,
            "phoneNum" to phone,
            "address" to address,
            "ekycStatus" to ekyc,
            "gender" to gender
        )

        if (dob != null) data["dateOfBirth"] = dob
        if (avtURL != null) data["avtURL"] = avtURL // Chỉ cập nhật field ảnh nếu có URL mới

        db.collection("customers")
            .document(uid)
            .update(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Cập nhật thành công.", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                btnUpdate.isEnabled = true
                Toast.makeText(this, "Cập nhật thất bại.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    // Thêm hàm nhận kết quả ảnh
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            imgAvatarPreview.setImageURI(imageUri)
        }
    }

    private fun loadCustomerData() {
        db.collection("customers")
            .document(intent.getStringExtra("uid") ?: "")
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    edtFullName.setText(document.getString("name"))
                    edtPhone.setText(document.getString("phoneNum"))
                    edtEmail.setText(document.getString("email"))
                    edtNationalId.setText(document.getString("nationalId"))
                    edtAddress.setText(document.getString("address"))
                    val dobTimestamp = document.getTimestamp("dateOfBirth")
                    if (dobTimestamp != null) {
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        edtDOB.setText(sdf.format(dobTimestamp.toDate()))
                    }

                    val currentAvt = document.getString("avtURL") ?: ""
                    Glide.with(this)
                        .load(currentAvt)
                        .placeholder(R.drawable.ic_user)
                        .into(imgAvatarPreview)

                    edtEKYC.setText(document.getString("ekycStatus"))
                    when (document.getString("gender")) {
                        "Nam" -> radioGender.check(R.id.radioMale)
                        "Nữ" -> radioGender.check(R.id.radioFemale)
                    }
                }
            }

    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // 👉 Nếu đã có DOB trong EditText → lấy từ DB
        if (edtDOB.text.toString().isNotEmpty()) {
            try {
                calendar.time = sdf.parse(edtDOB.text.toString())!!
            } catch (e: Exception) {
                // ignore
            }
        }

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
}