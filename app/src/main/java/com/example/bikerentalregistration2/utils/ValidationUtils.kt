package com.example.bikerentalregistration2.utils

import android.util.Patterns
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Simple input checks used by the sign-up and registration screens. */
object ValidationUtils {

    fun isValidEmail(email: String): Boolean =
        email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

    // Firebase requires at least 6 characters
    fun isValidPassword(password: String): Boolean = password.length >= 6

    // Indian mobile number: exactly 10 digits, starting with 6, 7, 8 or 9
    fun isValidPhone(phone: String): Boolean = Regex("^[6-9][0-9]{9}\$").matches(phone)

    // Date must be in dd/MM/yyyy format, a real calendar date, in the past, and after 1900
    fun isValidPastDate(dateText: String): Boolean {
        return try {
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            format.isLenient = false
            val date = format.parse(dateText)
            date != null && date.before(Date()) && date.time > -2208988800000L
        } catch (e: ParseException) {
            false
        }
    }
}
