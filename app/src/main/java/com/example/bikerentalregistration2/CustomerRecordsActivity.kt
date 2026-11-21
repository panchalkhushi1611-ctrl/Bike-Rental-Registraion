package com.example.bikerentalregistration2

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bikerentalregistration2.adapters.CustomerAdapter
import com.example.bikerentalregistration2.databinding.ActivityCustomerRecordsBinding
import com.example.bikerentalregistration2.utils.FirebaseUtils

/**
 * Shows customer records loaded from Firestore.
 *  - Admin: sees all records and gets the "Load Demo Records" button.
 *  - Normal user: sees only their own record (enforced by the Firestore rules too).
 */
class CustomerRecordsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerRecordsBinding
    private val adapter = CustomerAdapter()
    private var isAdmin = false
    private var uid = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerRecordsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = FirebaseUtils.currentUser()
        if (user == null) {
            finish()
            return
        }
        uid = user.uid

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.btnSeed.setOnClickListener { confirmSeed() }

        showLoading()
        FirebaseUtils.checkAdmin(uid) { admin ->
            if (isFinishing || isDestroyed) return@checkAdmin
            isAdmin = admin
            binding.btnSeed.visibility = if (admin) View.VISIBLE else View.GONE
            binding.tvSubtitle.text =
                if (admin) "Admin view: all customer records" else "Showing your registration"
            loadRecords()
        }
    }

    private fun loadRecords() {
        showLoading()
        FirebaseUtils.loadRecords(isAdmin, uid) { records, error ->
            if (isFinishing || isDestroyed) return@loadRecords
            binding.progressBar.visibility = View.GONE
            when {
                error != null -> showMessage(error)
                records.isNullOrEmpty() -> showMessage("No customer records found.")
                else -> {
                    binding.tvEmpty.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                    adapter.submitList(records ?: emptyList())
                }
            }
        }
    }

    private fun confirmSeed() {
        AlertDialog.Builder(this)
            .setTitle("Load demo records")
            .setMessage("This writes 5 fictional demo customers (demo_1 to demo_5). Running it again will not create duplicates.")
            .setPositiveButton("Load") { _, _ -> seedDemo() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun seedDemo() {
        binding.btnSeed.isEnabled = false
        showLoading()
        FirebaseUtils.seedDemoRecords { success, error ->
            if (isFinishing || isDestroyed) return@seedDemoRecords
            binding.btnSeed.isEnabled = true
            if (success) {
                Toast.makeText(this, "Demo records saved to Firestore.", Toast.LENGTH_SHORT).show()
                loadRecords()
            } else {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                loadRecords()
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        binding.recyclerView.visibility = View.GONE
    }

    private fun showMessage(message: String) {
        binding.recyclerView.visibility = View.GONE
        binding.tvEmpty.visibility = View.VISIBLE
        binding.tvEmpty.text = message
    }
}
