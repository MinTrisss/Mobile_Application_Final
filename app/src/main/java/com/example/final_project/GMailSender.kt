package com.example.final_project

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object GMailSender {

    private const val SENDER_EMAIL = "trongdeptrai146@gmail.com"
    private const val SENDER_PASSWORD = "qpyz mopa bfpo spcj"

    suspend fun sendOTPEmail(recipientEmail: String, otpCode: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val props = Properties().apply {
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "587")
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(SENDER_EMAIL))
                    setRecipient(Message.RecipientType.TO, InternetAddress(recipientEmail))
                    subject = "Mã xác thực OTP chuyển tiền"
                    setText("Mã OTP của bạn là: $otpCode\nMã này có hiệu lực trong 2 phút.\nVui lòng không chia sẻ cho bất kỳ ai.")
                }
                
                Transport.send(message)
                
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
