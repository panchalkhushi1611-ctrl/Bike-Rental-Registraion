package com.example.bikerentalregistration2.models

/**
 * One customer registration. Field names match the Firestore document fields.
 */
data class Customer(
    val uid: String = "",
    val customerId: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val dateOfBirth: String = "",
    val email: String = "",
    val address: String = "",
    val bikeCategory: String = "",
    val isDemo: Boolean = false
)
