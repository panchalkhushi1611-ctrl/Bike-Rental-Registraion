package com.example.bikerentalregistration2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bikerentalregistration2.utils.FirebaseUtils

/**
 * Splash / router screen. It decides where to go:
 *  - not logged in            -> LoginActivity
 *  - logged in, not registered -> CustomerFormActivity
 *  - logged in, registered     -> DashboardActivity
 * Firebase keeps the user logged in, so this is how the session is maintained.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (FirebaseUtils.currentUser() == null) {
            go(LoginActivity::class.java)
            return
        }

        FirebaseUtils.getMyCustomer { customer, error ->
            if (isFinishing || isDestroyed) return@getMyCustomer
            if (error != null) {
                // Could not check (for example offline): the dashboard shows the error with a Retry button
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                go(DashboardActivity::class.java)
            } else if (customer == null) {
                go(CustomerFormActivity::class.java)
            } else {
                go(DashboardActivity::class.java)
            }
        }
    }

    private fun go(target: Class<*>) {
        startActivity(Intent(this, target))
        finish()
    }
}
