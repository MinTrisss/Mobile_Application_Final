package com.example.final_project

import com.google.firebase.Timestamp

data class Customer(
    val uid: String = "",
    val customerId: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNum: String = "",
    val avtURL: String = "",
    val ekycStatus: String = "",
    val status: String = "",
    val role: String = "",
    val nationalId: String = "",
    val gender: String = "",
    val address: String = "",
    val dateOfBirth: Timestamp? = null,
    val createdAt: Timestamp? = null
)
