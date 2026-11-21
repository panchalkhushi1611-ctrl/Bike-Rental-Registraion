package com.example.bikerentalregistration2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.bikerentalregistration2.databinding.ActivityCustomerFormBinding
import com.example.bikerentalregistration2.utils.FirebaseUtils
import com.example.bikerentalregistration2.utils.ValidationUtils
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class CustomerFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerFormBinding
    private var isSubmitting = false   // blocks double taps / duplicate submissions
    private val bikeOptions = listOf("Scooter", "Commuter Bike", "Sports Bike")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (FirebaseUtils.currentUser() == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        prefillEmail()
        binding.actBike.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, bikeOptions)
        )
        binding.etDob.setOnClickListener { showDatePicker() }
        binding.tilDob.setEndIconOnClickListener { showDatePicker() }
        binding.btnSubmit.setOnClickListener { submitForm() }
        binding.btnClear.setOnClickListener { clearForm() }

        // If this user already registered, there is no need to show the form again
        setBusy(true)
        FirebaseUtils.getMyCustomer { customer, _ ->
            if (isFinishing || isDestroyed) return@getMyCustomer
            setBusy(false)
            if (customer != null) {
                Toast.makeText(this, "You have already registered.", Toast.LENGTH_LONG).show()
                goToDashboard()
            }
        }
    }

    private fun prefillEmail() {
        binding.etEmail.setText(FirebaseUtils.currentUser()?.email ?: "")
    }

    private fun showDatePicker() {
        // Only past dates can be selected
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())
            .build()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date of Birth")
            .setCalendarConstraints(constraints)
            .build()
        picker.addOnPositiveButtonClickListener { selection: Long ->
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")   // the picker returns UTC midnight
            binding.etDob.setText(format.format(Date(selection)))
            binding.tilDob.error = null
        }
        picker.show(supportFragmentManager, "DOB_PICKER")
    }

    private fun submitForm() {
        if (isSubmitting) return

        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val dob = binding.etDob.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val bike = binding.actBike.text.toString().trim().ifEmpty { "Not specified" }

        binding.tilName.error = null
        binding.tilPhone.error = null
        binding.tilDob.error = null
        binding.tilEmail.error = null
        binding.tilAddress.error = null

        var valid = true
        if (name.isEmpty()) {
            binding.tilName.error = "Full name is required"
            valid = false
        }
        if (!ValidationUtils.isValidPhone(phone)) {
            binding.tilPhone.error = "Enter a valid 10-digit mobile number"
            valid = false
        }
        if (!ValidationUtils.isValidPastDate(dob)) {
            binding.tilDob.error = "Select a valid date of birth (in the past)"
            valid = false
        }
        if (!ValidationUtils.isValidEmail(email)) {
            binding.tilEmail.error = "Enter a valid email address"
            valid = false
        }
        if (address.isEmpty()) {
            binding.tilAddress.error = "Address is required"
            valid = false
        }
        if (!valid) return

        setBusy(true)
        FirebaseUtils.saveCustomer(name, phone, dob, email, address, bike) { success, customerId, error ->
            if (isFinishing || isDestroyed) return@saveCustomer
            setBusy(false)
            if (success) {
                AlertDialog.Builder(this)
                    .setTitle("Registration Successful")
                    .setMessage("Your Registration ID is:\n\n$customerId")
                    .setCancelable(false)
                    .setPositiveButton("Go to Dashboard") { _, _ -> goToDashboard() }
                    .show()
            } else {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun clearForm() {
        binding.etName.setText("")
        binding.etPhone.setText("")
        binding.etDob.setText("")
        binding.etAddress.setText("")
        binding.actBike.setText("", false)
        prefillEmail()
        binding.tilName.error = null
        binding.tilPhone.error = null
        binding.tilDob.error = null
        binding.tilEmail.error = null
        binding.tilAddress.error = null
    }

    private fun setBusy(busy: Boolean) {
        isSubmitting = busy
        binding.progressBar.visibility = if (busy) View.VISIBLE else View.GONE
        binding.btnSubmit.isEnabled = !busy
        binding.btnClear.isEnabled = !busy
    }

    private fun goToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
