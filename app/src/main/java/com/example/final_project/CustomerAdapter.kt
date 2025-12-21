package com.example.final_project

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import android.util.Log


class CustomerAdapter(
    private val list: List<Customer>,
    private val onEdit: (Customer) -> Unit,
    private val onToggleStatus: (Customer) -> Unit,
    private val onItemClick: (Customer) -> Unit,
) : RecyclerView.Adapter<CustomerAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
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
        h.txtMeta.text = "${c.nationalId} · ${c.phoneNum}"
        h.txtStatus.text = "Trạng thái: ${c.status}"

        Log.d("DEBUG", "nationalId = ${c.nationalId}")

        h.txtStatus.setTextColor(
            when (c.ekycStatus) {
                "Hoạt động" -> Color.parseColor("#2E7D32")
                "Đã khoá" -> Color.parseColor("#F57C00")
                else -> Color.GRAY
            }
        )

        h.btnEdit.setOnClickListener { onEdit(c) }
        h.btnDelete.setOnClickListener { onToggleStatus(c) }
        h.itemView.setOnClickListener { onItemClick(c) }

    }

    override fun getItemCount(): Int = list.size
}
