package com.example.final_project

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

data class Provider(val name: String, val logo: Int, val targetActivityClass: Class<*>)

class ProviderSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_provider_selection)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val serviceType = intent.getStringExtra("serviceType")
        
        supportActionBar?.title = when (serviceType) {
            "Điện" -> "Chọn Nhà Cung Cấp Điện"
            "Nước" -> "Chọn Nhà Cung Cấp Nước"
            "Nạp tiền" -> "Chọn Nhà Mạng Di Động"
            else -> "Chọn Nhà Cung Cấp"
        }

        val providers = getProvidersForService(serviceType)
        
        val rvProviders: RecyclerView = findViewById(R.id.rvProviders)
        rvProviders.layoutManager = LinearLayoutManager(this)
        rvProviders.adapter = ProviderAdapter(providers)
    }

    private fun getProvidersForService(serviceType: String?): List<Provider> {
        return when (serviceType) {
            "Điện" -> listOf(
                Provider("EVN HCMC", android.R.drawable.ic_dialog_alert, ElectricityPaymentActivity::class.java),
                Provider("EVN Hanoi", android.R.drawable.ic_dialog_alert, ElectricityPaymentActivity::class.java),
                Provider("EVN Da Nang", android.R.drawable.ic_dialog_alert, ElectricityPaymentActivity::class.java)
            )
            "Nước" -> listOf(
                Provider("Sawa HCMC", android.R.drawable.ic_menu_crop, WaterPaymentActivity::class.java),
                Provider("Hanoi Water", android.R.drawable.ic_menu_crop, WaterPaymentActivity::class.java),
                Provider("Da Nang Water", android.R.drawable.ic_menu_crop, WaterPaymentActivity::class.java)
            )
            "Nạp tiền" -> listOf(
                Provider("Viettel", android.R.drawable.ic_menu_call, PhoneTopUpActivity::class.java),
                Provider("MobiFone", android.R.drawable.ic_menu_call, PhoneTopUpActivity::class.java),
                Provider("VinaPhone", android.R.drawable.ic_menu_call, PhoneTopUpActivity::class.java),
                Provider("Vietnamobile", android.R.drawable.ic_menu_call, PhoneTopUpActivity::class.java)
            )
            else -> emptyList()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

class ProviderAdapter(private val providers: List<Provider>) : RecyclerView.Adapter<ProviderAdapter.ProviderViewHolder>() {

    class ProviderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val logo: ImageView = view.findViewById(R.id.ivProviderLogo)
        val name: TextView = view.findViewById(R.id.tvProviderName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProviderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_provider, parent, false)
        return ProviderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProviderViewHolder, position: Int) {
        val provider = providers[position]
        holder.name.text = provider.name
        holder.logo.setImageResource(provider.logo)
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, provider.targetActivityClass)
            intent.putExtra("providerName", provider.name)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = providers.size
}
