package com.example.final_project

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class LoanAdapter(
    private val loans: List<Loan>,
    private val isEmployee: Boolean,
    private val onStatusClick: (Loan) -> Unit
    ) :
    RecyclerView.Adapter<LoanAdapter.LoanViewHolder>() {

    class LoanViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLoanId: TextView = view.findViewById(R.id.tvLoanId)
        val tvLoanStatus: TextView = view.findViewById(R.id.tvLoanStatus)
        val tvLoanAmount: TextView = view.findViewById(R.id.tvLoanAmount)
        val tvLoanDuration: TextView = view.findViewById(R.id.tvLoanDuration)
        val tvMonthlyPayment: TextView = view.findViewById(R.id.tvMonthlyPayment)
        val tvLoanDate: TextView = view.findViewById(R.id.tvLoanDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoanViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_loan, parent, false)
        return LoanViewHolder(view)
    }

    override fun onBindViewHolder(holder: LoanViewHolder, position: Int) {
        val loan = loans[position]
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        if (isEmployee) {
            holder.itemView.setOnClickListener { onStatusClick(loan) }
            // Có thể thêm icon gợi ý nhấn để sửa
        }
        when (loan.status.lowercase().trim()) {
            "chấp nhận" -> {
                holder.tvLoanStatus.text = "Chấp nhận"
                holder.tvLoanStatus.setBackgroundResource(R.drawable.bg_status_success)
            }
            "từ chối" -> {
                holder.tvLoanStatus.text = "Từ chối"
                holder.tvLoanStatus.setBackgroundResource(R.drawable.bg_status_rejected)
            }
            else -> { // "chờ duyệt" hoặc các trường hợp khác
                holder.tvLoanStatus.text = "Chờ duyệt"
                holder.tvLoanStatus.setBackgroundResource(R.drawable.bg_status_pending)
            }
        }

        holder.tvLoanId.text = "Khoản vay #${loan.id.takeLast(6)}"

        holder.tvLoanAmount.text =
            "Số tiền vay: ${String.format("%,.0f", loan.principal)} VNĐ"

        holder.tvLoanDuration.text =
            "Kỳ hạn: ${loan.termMonths} tháng"

        holder.tvMonthlyPayment.text =
            "Trả mỗi tháng: ${String.format("%,.0f", loan.monthlyPayment)} VNĐ"

        holder.tvLoanDate.text =
            "Ngày đăng ký: ${sdf.format(loan.createdAt)}"
    }


    override fun getItemCount() = loans.size
}
