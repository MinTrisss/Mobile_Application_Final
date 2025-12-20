package com.example.final_project

import java.util.Date

data class Loan(
    val id: String,
    val uid: String,
    val principal: Double,
    val interestRate: Double,
    val termMonths: Int,
    val monthlyPayment: Double,
    val totalPayment: Double,
    val totalInterest: Double,
    val status: String,
    val createdAt: Date
)


enum class PaymentFrequency {
    MONTHLY,
    BI_WEEKLY
}

fun calculatePeriodicPayment(
    principal: Double,
    annualRate: Double,
    termMonths: Int,
    frequency: PaymentFrequency
): Double {

    val periods: Int
    val ratePerPeriod: Double

    when (frequency) {
        PaymentFrequency.MONTHLY -> {
            periods = termMonths
            ratePerPeriod = annualRate / 12
        }
        PaymentFrequency.BI_WEEKLY -> {
            periods = Math.round(termMonths / 12.0 * 26).toInt()
            ratePerPeriod = annualRate / 26
        }
    }

    if (ratePerPeriod == 0.0 || periods == 0) {
        return principal / periods.coerceAtLeast(1)
    }

    return principal *
            ratePerPeriod *
            Math.pow(1 + ratePerPeriod, periods.toDouble()) /
            (Math.pow(1 + ratePerPeriod, periods.toDouble()) - 1)
}

