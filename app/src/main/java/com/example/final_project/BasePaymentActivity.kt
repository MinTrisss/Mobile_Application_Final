package com.example.final_project

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

abstract class BasePaymentActivity : AppCompatActivity() {

    protected lateinit var auth: FirebaseAuth
    protected lateinit var db: FirebaseFirestore

    companion object {
        protected const val RC_VNPAY_GATEWAY = 1102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    protected fun launchVnPayGateway(amount: Double, orderInfo: String) {
        val paymentUrl = createVnPayUrl(amount, orderInfo)
        val intent = Intent(this, VnPayGatewayActivity::class.java)
        intent.putExtra("paymentUrl", paymentUrl)
        startActivityForResult(intent, RC_VNPAY_GATEWAY)
    }

    private fun createVnPayUrl(amount: Double, orderInfo: String): String {
        val tmnCode = "ZSQSX340"
        val hashSecret = "5TZ2SIGG07KLPOJNUTN9BAWOBRF487T5"
        val returnUrl = "https://my-app.com/vnpay_return"

        val vnpParams = mutableMapOf<String, String>()
        vnpParams["vnp_Version"] = "2.1.0"
        vnpParams["vnp_Command"] = "pay"
        vnpParams["vnp_TmnCode"] = tmnCode
        vnpParams["vnp_Amount"] = (amount.toLong() * 100).toString()
        vnpParams["vnp_CurrCode"] = "VND"
        vnpParams["vnp_TxnRef"] = System.currentTimeMillis().toString()
        vnpParams["vnp_OrderInfo"] = orderInfo
        vnpParams["vnp_OrderType"] = "other"
        vnpParams["vnp_Locale"] = "vn"
        vnpParams["vnp_ReturnUrl"] = returnUrl
        vnpParams["vnp_IpAddr"] = "127.0.0.1"

        val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
        vnpParams["vnp_CreateDate"] = sdf.format(Date())
        
        val fieldNames = vnpParams.keys.sorted()
        val hashData = StringBuilder()
        fieldNames.forEach { fieldName ->
            val fieldValue = vnpParams[fieldName]
            if (fieldValue != null && fieldValue.isNotEmpty()) {
                hashData.append(fieldName)
                hashData.append('=')
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()))
                hashData.append('&')
            }
        }
        hashData.setLength(hashData.length - 1)

        val secureHash = hmacSHA512(hashSecret, hashData.toString())
        vnpParams["vnp_SecureHash"] = secureHash

        val queryUrl = vnpParams.entries.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, StandardCharsets.US_ASCII.toString())}=${URLEncoder.encode(value, StandardCharsets.US_ASCII.toString())}"
        }

        return "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?$queryUrl"
    }

    private fun hmacSHA512(key: String, data: String): String {
        val mac = Mac.getInstance("HmacSHA512")
        val secretKeySpec = SecretKeySpec(key.toByteArray(), "HmacSHA512")
        mac.init(secretKeySpec)
        val hashBytes = mac.doFinal(data.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }.toUpperCase(Locale.ROOT)
    }
    
    protected abstract fun onPaymentSuccess(amount: Double)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_VNPAY_GATEWAY) {
            if (resultCode == Activity.RESULT_OK) {
                val amount = intent.getDoubleExtra("amount", 0.0) // Lấy lại số tiền từ intent ban đầu
                onPaymentSuccess(amount)
            } else {
                Toast.makeText(this, "Thanh toán đã bị hủy hoặc thất bại.", Toast.LENGTH_LONG).show()
            }
        }
    }

    protected fun performTransaction(amount: Double, note: String, type: String, toAccountId: String) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("accounts")
            .whereEqualTo("customerId", userId)
            .whereEqualTo("type", "checking")
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if(snapshot.isEmpty){
                    Toast.makeText(this, "Lỗi: Không tìm thấy tài khoản thanh toán.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val checkingAccountRef = snapshot.documents[0].reference

                db.runTransaction { transaction ->
                    val checkingSnap = transaction.get(checkingAccountRef)
                    val currentBalance = checkingSnap.getDouble("balance") ?: 0.0

                    if (currentBalance < amount) {
                        throw Exception("Số dư không đủ để thanh toán.")
                    }

                    transaction.update(checkingAccountRef, "balance", currentBalance - amount)

                    val transactionId = "T${System.currentTimeMillis()}"
                    val transactionRecord = hashMapOf(
                        "transactionId" to transactionId,
                        "customerId" to userId,
                        "fromAccountId" to (checkingSnap.getString("accountId") ?: ""),
                        "toAccountId" to toAccountId,
                        "amount" to amount,
                        "note" to note,
                        "status" to "Thành công",
                        "timestamp" to Timestamp.now(),
                        "type" to type
                    )
                    transaction.set(db.collection("transactions").document(), transactionRecord)

                    null
                }.addOnSuccessListener {
                    Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_LONG).show()
                    finish()
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Thanh toán thất bại: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}
