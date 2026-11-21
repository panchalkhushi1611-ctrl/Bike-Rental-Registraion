package com.example.bikerentalregistration2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bikerentalregistration2.databinding.ActivitySignupBinding
import com.example.bikerentalregistration2.utils.FirebaseUtils
import com.example.bikerentalregistration2.utils.ValidationUtils

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignup.setOnClickListener { attemptSignup() }
        binding.btnBackToLogin.setOnClickListener { finish() }
    }

    private fun attemptSignup() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirm = binding.etConfirm.text.toString()

        binding.tilName.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirm.error = null

        var valid = true
        if (name.isEmpty()) {
            binding.tilName.error = "Full name is required"
            valid = false
        }
        if (!ValidationUtils.isValidEmail(email)) {
            binding.tilEmail.error = "Enter a valid email address"
            valid = false
        }
        if (!ValidationUtils.isValidPassword(password)) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            valid = false
        }
        if (confirm != password) {
            binding.tilConfirm.error = "Passwords do not match"
            valid = false
        }
        if (!valid) return

        setLoading(true)
        FirebaseUtils.signUp(name, email, password) { success, error ->
            if (isFinishing || isDestroyed) return@signUp
            setLoading(false)
            if (success) {
                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                // Firebase logs the new user in automatically; MainActivity opens the registration form
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSignup.isEnabled = !loading
        binding.btnBackToLogin.isEnabled = !loading
    }
}
