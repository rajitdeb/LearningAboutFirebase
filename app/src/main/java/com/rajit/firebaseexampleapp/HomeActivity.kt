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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.rajit.firebaseexampleapp.databinding.ActivityHomeBinding
import com.rajit.firebaseexampleapp.model.UserInfo
import com.rajit.firebaseexampleapp.util.toUserInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var _binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        auth = Firebase.auth
        db = Firebase.firestore

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

        _binding.addUserDataBtn.setOnClickListener {
            // Navigate to Add User Data Activity
            navigateToAddUserActivity()
        }

        _binding.updatedUserFullNameBtn.setOnClickListener {
            // Update User's Full Name
//            updateUserFullName()

            // Updating multiple selective fields without updating/overwriting the entire document
            updateEmailAndFullName()
        }

        _binding.getDataOnce.setOnClickListener { getDataOnce() }

    }

    private fun updateEmailAndFullName() {
        CoroutineScope(Dispatchers.IO).launch {
            db.collection("users")
                .document(auth.currentUser!!.uid)
                .update(
                    mapOf(
                        "fullName" to "Maiden Name",
                        "email" to "updatedEmail@gmail.com"
                    )
                )
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(applicationContext, "Update Success", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
        }
    }

    private fun updateUserFullName() {
        // Here, i'm statically changing the user's full name
        CoroutineScope(Dispatchers.IO).launch {
            db.collection("users")
                .document(auth.currentUser!!.uid)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot != null) {

                        val data = documentSnapshot.toUserInfo()

                        // Updating single field
                        val updatedName = hashMapOf("fullName" to data.fullName.lowercase())

                        // Posting it to the firestore
                        db.collection("users")
                            .document(auth.currentUser!!.uid)
                            .set(updatedName, SetOptions.merge())
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(
                                        applicationContext,
                                        "User: ${task.result}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    }
                }
        }
    }

    private fun getDataOnce() {
        CoroutineScope(Dispatchers.IO).launch {
            db.collection("users")
                .document(auth.currentUser!!.uid)
                .get()
                .addOnSuccessListener { documentSnapshot ->

                    if (documentSnapshot != null) {

                        // Mapping Document Snapshot to UserInfo Model using Extension Function
                        val data = documentSnapshot.toUserInfo()

                        Toast.makeText(
                            applicationContext,
                            "User: $data",
                            Toast.LENGTH_SHORT
                        ).show()

                    }
                }
        }
    }

    private fun disableAllButtons(disable: Boolean) {

        // disable = true - Disable all buttons
        // disable = false - Enable all buttons

        _binding.apply {
            logoutBtn.isEnabled = !disable
        }
    }

    private fun navigateToAddUserActivity() {
        val intent = Intent(this@HomeActivity, AddUserActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToLoginActivity() {
        val intent = Intent(this@HomeActivity, MainActivity::class.java)
        startActivity(intent)

        // Clear Back Stack
        finishAffinity()
    }
}