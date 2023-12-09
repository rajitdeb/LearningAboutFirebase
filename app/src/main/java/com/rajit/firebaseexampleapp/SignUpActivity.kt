package com.rajit.firebaseexampleapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.rajit.firebaseexampleapp.databinding.ActivitySignUpBinding
import com.rajit.firebaseexampleapp.model.UserInfo
import com.rajit.firebaseexampleapp.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val SIGNUPACTIVITY_TAG = "SignUpActivity"

class SignUpActivity : AppCompatActivity() {

    private lateinit var _binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        auth = Firebase.auth
        db = Firebase.firestore

        _binding.apply {

            emailSignUpBtn.setOnClickListener {

                // Sign Up using Email
                signUpUsingEmail()
            }

        }

    }

    private fun disableAllButtons(disable: Boolean) {

        // disable = true - Disable all buttons
        // disable = false - Enable all buttons

        _binding.apply {
            emailSignUpBtn.isEnabled = !disable
            loginBtn.isEnabled = !disable
        }
    }

    private fun signUpUsingEmail() {

        _binding.apply {

            // Show Loading Progress Bar
            loadingProgress.visibility = View.VISIBLE

            // Disable all buttons while Sign up is in Progress
            disableAllButtons(true)

            val fullName = nameEdt.text.trim().toString()
            val email = emailAddressEdt.text.trim().toString()
            val password = passwordEdt.text.trim().toString()
            val confirmPassword = confirmPasswordEdt.text.trim().toString()

            // Validate Sign Up Fields - Name, Email, Password, Confirm Password
            val isValidField = validateSignUpFields(email, password, confirmPassword)

            if (isValidField) {
                // Start the signup process
                startSignUpProcessUsingEmailPassword(fullName, email, password)
            }

        }

    }

    private fun startSignUpProcessUsingEmailPassword(
        fullName: String,
        email: String,
        password: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            auth
                .createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->

                    if (task.isSuccessful) {

                        if (auth.currentUser != null) {

                            // Create a model to save to UserInfo
                            val userInfo = UserInfo(
                                fullName,
                                email,
                                auth.currentUser!!.uid
                            )

                            // Save User Details to DB
                            saveUserDetailsToDB(userInfo)

                            // On Successful Save, Login After Successful Sign Up
                            loginUserWithEmailAndPassword(email, password)
                        }
                    } else {

                        // Stop Loading - Hide Loading Progress Bar
                        _binding.loadingProgress.visibility = View.GONE

                        Log.i(
                            SIGNUPACTIVITY_TAG,
                            "startSignUpProcessUsingEmailPassword: Error: ${task.exception}"
                        )

                        Toast.makeText(
                            applicationContext,
                            "Error: ${task.exception}",
                            Toast.LENGTH_SHORT
                        ).show()

                    }
                }
        }
    }

    private fun loginUserWithEmailAndPassword(email: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->

                    // Stop Loading - Hide Progress Bar
                    _binding.loadingProgress.visibility = View.GONE

                    // Enable all Buttons
                    disableAllButtons(false)

                    if (task.isSuccessful) {

                        Toast.makeText(
                            applicationContext,
                            "Login Successful :)",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        Log.i(
                            SIGNUPACTIVITY_TAG,
                            "startSignUpProcessUsingEmailPassword: Error: ${task.exception}"
                        )

                        Toast.makeText(
                            applicationContext,
                            "Error: ${task.exception}",
                            Toast.LENGTH_SHORT
                        ).show()

                    }
                }
        }
    }

    private fun saveUserDetailsToDB(userInfo: UserInfo) {
        CoroutineScope(Dispatchers.IO).launch {
            db.collection(Constants.USER_COLLECTION)
                .document(userInfo.uId)
                .set(userInfo)
                .addOnSuccessListener {
                    Log.i(
                        SIGNUPACTIVITY_TAG,
                        "saveUserDetailsToDB: Successfully saved User to DB"
                    )
                }
                .addOnFailureListener { exception ->
                    Log.i(
                        SIGNUPACTIVITY_TAG,
                        "saveUserDetailsToDB: Error: $exception"
                    )

                    Toast.makeText(
                        applicationContext,
                        "Error: $exception",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun validateSignUpFields(
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {

        // Check if any of the field is empty
        if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {

            // Check if the password and confirm password is same
            return if (password == confirmPassword) {
                true
            } else {
                Toast.makeText(applicationContext, "Passwords do no match!", Toast.LENGTH_SHORT)
                    .show()
                false
            }

        }

        return false
    }
}