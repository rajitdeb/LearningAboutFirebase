package com.rajit.firebaseexampleapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.rajit.firebaseexampleapp.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var _binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        auth = Firebase.auth

        _binding.logoutBtn.setOnClickListener {

            // Disable all buttons
            disableAllButtons(true)

            // Log out the current user
            auth.signOut()

            // Notify the user about logout
            Toast.makeText(
                applicationContext,
                "Log Out Successful :)",
                Toast.LENGTH_SHORT
            ).show()

            // Enable Buttons after successful logout
            disableAllButtons(false)

            // Navigate the user to Login Activity
            navigateToLoginActivity()
        }

    }

    private fun disableAllButtons(disable: Boolean) {

        // disable = true - Disable all buttons
        // disable = false - Enable all buttons

        _binding.apply {
            logoutBtn.isEnabled = !disable
        }
    }

    private fun navigateToLoginActivity() {
        val intent = Intent(this@HomeActivity, MainActivity::class.java)
        startActivity(intent)

        // Clear Back Stack
        finishAffinity()
    }
}