package com.example.final_project

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.random.Random

class TicketDetailsActivity : AppCompatActivity() {

    private lateinit var layoutTicket: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket_details)

        // Nhận dữ liệu từ Intent
        val movieTitle = intent.getStringExtra("movieTitle") ?: ""
        val seats = intent.getStringExtra("seats") ?: ""
        val cinema = intent.getStringExtra("cinema") ?: ""
        val showtime = intent.getStringExtra("showtime") ?: ""
        val room = intent.getStringExtra("room") ?: ""

        // Ánh xạ view
        layoutTicket = findViewById(R.id.layoutTicket)
        val tvMovieTitle: TextView = findViewById(R.id.tvMovieTitle)
        val tvCinemaName: TextView = findViewById(R.id.tvCinemaName)
        val tvSeats: TextView = findViewById(R.id.tvSeats)
        val tvShowtime: TextView = findViewById(R.id.tvShowtime)
        val tvRoom: TextView = findViewById(R.id.tvRoom)
        val ivQrCode: ImageView = findViewById(R.id.ivQrCode)
        val btnSaveTicket: Button = findViewById(R.id.btnSaveTicket)

        // Gán dữ liệu
        tvMovieTitle.text = movieTitle
        tvCinemaName.text = cinema
        tvSeats.text = seats
        tvShowtime.text = showtime
        tvRoom.text = room

        // QR giả lập
        ivQrCode.setImageBitmap(createFakeQrCode(400))

        // Sự kiện lưu vé
        btnSaveTicket.setOnClickListener {
            saveTicketAsImage()
        }
    }

    /**
     * Tạo QR giả để demo
     */
    private fun createFakeQrCode(size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val blockSize = 20

        for (x in 0 until size step blockSize) {
            for (y in 0 until size step blockSize) {
                val color = if (Random.nextBoolean()) Color.BLACK else Color.WHITE
                for (i in x until x + blockSize) {
                    for (j in y until y + blockSize) {
                        if (i < size && j < size) {
                            bitmap.setPixel(i, j, color)
                        }
                    }
                }
            }
        }
        return bitmap
    }

    /**
     * Lưu layout vé thành ảnh PNG
     */
    private fun saveTicketAsImage() {
        val bitmap = Bitmap.createBitmap(
            layoutTicket.width,
            layoutTicket.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        layoutTicket.draw(canvas)

        val filename = "ticket_${System.currentTimeMillis()}.png"
        var outputStream: OutputStream? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/TTK_Tickets"
                    )
                }

                val uri = contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )

                if (uri != null) {
                    outputStream = contentResolver.openOutputStream(uri)
                }
            } else {
                val imagesDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val imageFile = File(imagesDir, filename)
                outputStream = FileOutputStream(imageFile)
            }

            outputStream?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                Toast.makeText(
                    this,
                    "Đã lưu vé vào thư viện ảnh",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Lưu vé thất bại",
                Toast.LENGTH_SHORT
            ).show()
            e.printStackTrace()
        }
    }
}
