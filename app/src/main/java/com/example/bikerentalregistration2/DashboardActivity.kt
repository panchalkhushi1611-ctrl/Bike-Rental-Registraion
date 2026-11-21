package com.example.bikerentalregistration2

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.bikerentalregistration2.databinding.ActivityDashboardBinding
import com.example.bikerentalregistration2.models.Customer
import com.example.bikerentalregistration2.utils.FirebaseUtils
import com.google.firebase.auth.FirebaseUser

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRecords.setOnClickListener {
            startActivity(Intent(this, CustomerRecordsActivity::class.java))
        }
        binding.btnNewReg.setOnClickListener {
            startActivity(Intent(this, CustomerFormActivity::class.java))
        }
        binding.btnLogout.setOnClickListener { logout() }
        binding.btnRetry.setOnClickListener { loadData() }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        val user = FirebaseUtils.currentUser()
        if (user == null) {
            goToLogin()
            return
        }
        showLoading()
        FirebaseUtils.getMyCustomer { customer, error ->
            if (isFinishing || isDestroyed) return@getMyCustomer
            if (error != null) showError(error) else showContent(customer, user)
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.layoutError.visibility = View.GONE
        binding.layoutContent.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.layoutContent.visibility = View.GONE
        binding.layoutError.visibility = View.VISIBLE
        binding.tvError.text = message
    }

    private fun showContent(customer: Customer?, user: FirebaseUser) {
        binding.progressBar.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
        binding.layoutContent.visibility = View.VISIBLE

        if (customer != null) {
            binding.tvWelcome.text = "Welcome, ${customer.fullName}"
            binding.tvStatus.text = "Registered ✔"
            binding.cardDetails.visibility = View.VISIBLE
            binding.tvCustomerId.text = "Registration ID: ${customer.customerId}"
            binding.tvPhone.text = "Phone: ${customer.phoneNumber}"
            binding.tvDob.text = "Date of Birth: ${customer.dateOfBirth}"
            binding.tvEmail.text = "Email: ${customer.email}"
            binding.tvAddress.text = "Address: ${customer.address}"
            binding.tvBike.text = "Bike Category: ${customer.bikeCategory}"
            binding.btnNewReg.visibility = View.GONE
        } else {
            val name = user.displayName?.takeIf { it.isNotBlank() } ?: user.email ?: "Customer"
            binding.tvWelcome.text = "Welcome, $name"
            binding.tvStatus.text = "Not registered yet"
            binding.cardDetails.visibility = View.GONE
            binding.btnNewReg.visibility = View.VISIBLE
        }
    }

    private fun logout() {
        FirebaseUtils.logout()
        goToLogin()
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
