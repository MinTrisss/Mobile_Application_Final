package com.example.final_project

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.GridLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat

data class Movie(val title: String, val price: Double)
data class Showtime(val time: String, val room: String)

class MovieTicketPaymentActivity : BasePaymentActivity() {

    private lateinit var spinnerMovies: Spinner
    private lateinit var spinnerShowtimes: Spinner
    private lateinit var gridSeats: GridLayout
    private lateinit var tvMovieTicketAmount: TextView
    private lateinit var btnPayMovieTicket: Button

    private val mockMovies = listOf(
        Movie("Lật Mặt 7", 110000.0),
        Movie("Doraemon: Nobita và Bản giao hưởng Địa Cầu", 95000.0),
        Movie("Godzilla x Kong: The New Empire", 120000.0)
    )
    private val mockShowtimes = listOf(
        Showtime("18:30", "05"),
        Showtime("20:00", "02"),
        Showtime("21:45", "03")
    )

    private var totalAmount = 0.0
    private val selectedSeats = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie_ticket_payment)

        val toolbar: Toolbar = findViewById(R.id.toolbarMovieTicketPayment)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Mua vé xem phim"
            setDisplayHomeAsUpEnabled(true)
        }

        initViews()
        setupSpinners()
        createSeats()
        initActions()
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

    private fun initViews() {
        spinnerMovies = findViewById(R.id.spinnerMovies)
        spinnerShowtimes = findViewById(R.id.spinnerShowtimes)
        gridSeats = findViewById(R.id.gridSeats)
        tvMovieTicketAmount = findViewById(R.id.tvMovieTicketAmount)
        btnPayMovieTicket = findViewById(R.id.btnPayMovieTicket)
    }

    private fun setupSpinners() {
        val movieTitles = mockMovies.map { it.title }
        val movieAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, movieTitles)
        movieAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMovies.adapter = movieAdapter

        val showtimeStrings = mockShowtimes.map { it.time }
        val showtimeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, showtimeStrings)
        showtimeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerShowtimes.adapter = showtimeAdapter

        val itemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (selectedSeats.isNotEmpty()) {
                    resetSeatSelection()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinnerMovies.onItemSelectedListener = itemSelectedListener
        spinnerShowtimes.onItemSelectedListener = itemSelectedListener
    }

    private fun createSeats() {
        gridSeats.removeAllViews()
        val totalSeats = 20
        for (i in 1..totalSeats) {
            val seatView = TextView(this).apply {
                val rowChar = ('A'.code + (i - 1) / 5).toChar()
                val seatNum = (i - 1) % 5 + 1
                text = "$rowChar$seatNum"
                
                gravity = Gravity.CENTER
                setPadding(16, 16, 16, 16)
                background = ContextCompat.getDrawable(this@MovieTicketPaymentActivity, R.drawable.bg_seat_available)
                setOnClickListener { onSeatClick(this) }
            }
            gridSeats.addView(seatView)
        }
    }

    private fun onSeatClick(seatView: TextView) {
        val seatName = seatView.text.toString()
        if (selectedSeats.contains(seatName)) {
            selectedSeats.remove(seatName)
            seatView.background = ContextCompat.getDrawable(this, R.drawable.bg_seat_available)
        } else {
            selectedSeats.add(seatName)
            seatView.background = ContextCompat.getDrawable(this, R.drawable.bg_seat_selected)
        }
        calculateTotal()
    }

    private fun initActions() {
        btnPayMovieTicket.setOnClickListener {
            if (totalAmount > 0) {
                val selectedMovie = spinnerMovies.selectedItem.toString()
                val orderInfo = "Mua ${selectedSeats.size} ve phim $selectedMovie"
                intent.putExtra("amount", totalAmount)
                launchVnPayGateway(totalAmount, orderInfo)
            } else {
                Toast.makeText(this, "Vui lòng chọn ít nhất 1 ghế", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun calculateTotal() {
        val selectedMoviePrice = mockMovies[spinnerMovies.selectedItemPosition].price
        totalAmount = selectedMoviePrice * selectedSeats.size

        tvMovieTicketAmount.text = "Tổng tiền: ${String.format("%,.0f", totalAmount)} VNĐ"
        btnPayMovieTicket.isEnabled = totalAmount > 0
    }

    private fun resetSeatSelection() {
        selectedSeats.clear()
        createSeats()
        calculateTotal()
    }

    override fun onPaymentSuccess(amount: Double) {
        val selectedMovie = mockMovies[spinnerMovies.selectedItemPosition]
        val selectedShowtime = mockShowtimes[spinnerShowtimes.selectedItemPosition]
        val note = "Mua ${selectedSeats.size} ve phim ${selectedMovie.title}"
        
        val intent = Intent(this, TicketDetailsActivity::class.java).apply {
            putExtra("movieTitle", selectedMovie.title)
            putExtra("seats", selectedSeats.joinToString(", "))
            putExtra("showtime", selectedShowtime.time)
            putExtra("room", selectedShowtime.room)
            putExtra("cinema", "TTK Cinema")
        }
        startActivity(intent)

        performTransaction(amount, note, "movie_ticket", "CGV")
        
        finish()
    }
}
