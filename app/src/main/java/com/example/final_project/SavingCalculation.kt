package com.example.final_project

data class SavingCalculation(
    val principal: Double,
    val term: String,
    val termMonths: Int,
    val interestRate: Double,
    val monthlyInterest: Double,
    val totalInterest: Double,
    val totalAtMaturity: Double
)