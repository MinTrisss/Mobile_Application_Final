package com.example.final_project

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class CustomerAdapter(
    private val list: List<Customer>,
    private val onEdit: (Customer) -> Unit,
    private val onToggleStatus: (Customer) -> Unit,
    val onItemClick: (Customer) -> Unit,
) : RecyclerView.Adapter<CustomerAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val imgAvatar: ImageView = v.findViewById(R.id.imgCustomerAvatar)
        val txtName: TextView = v.findViewById(R.id.txtCustomerName)
        val txtMeta: TextView = v.findViewById(R.id.txtCustomerMeta)
        val txtStatus: TextView = v.findViewById(R.id.txtCustomerStatus)
        val btnDelete: Button = v.findViewById(R.id.btnDelete)
        val btnEdit: Button = v.findViewById(R.id.btnEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_customer, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val c = list[position]

        h.txtName.text = c.name
        h.txtMeta.text = "${c.customerId} · ${c.phoneNum}"
        h.txtStatus.text = "Available: ${c.status}"

        h.txtStatus.setTextColor(
            when (c.ekycStatus) {
                "normal" -> Color.parseColor("#2E7D32")
                "locked" -> Color.parseColor("#F57C00")
                else -> Color.GRAY
            }
        )

        Glide.with(h.itemView.context)
            .load(c.avtURL)
            .placeholder(R.drawable.ic_user)
            .error(R.drawable.ic_user)
            .into(h.imgAvatar)

        h.btnEdit.setOnClickListener { onEdit(c) }
        h.btnDelete.setOnClickListener { onToggleStatus(c) }
        h.itemView.setOnClickListener { onItemClick(c) }

    }

    override fun getItemCount(): Int = list.size
}
