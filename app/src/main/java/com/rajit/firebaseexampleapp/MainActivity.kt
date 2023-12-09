package com.rajit.firebaseexampleapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.rajit.firebaseexampleapp.databinding.ActivityMainBinding
import com.rajit.firebaseexampleapp.model.UserInfo
import com.rajit.firebaseexampleapp.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val LOGINACTIVITY_TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var _binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    private var googleSignInResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        if (result.resultCode == RESULT_OK) {
            // Step 4: Get the user data from sign in intent
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

            try {
                val account = task.result
                val idToken = account.idToken

                signInUserWithGoogleIDToken(idToken)

            } catch (e: Exception) {
                Log.e(LOGINACTIVITY_TAG, "googleSignIn: Error: $e")

                // Notify user about Login Failure
                Toast.makeText(
                    applicationContext,
                    "Login failed! Error: $e",
                    Toast.LENGTH_SHORT
                ).show()

                // Hide Loading - Hide Progress Bar
                _binding.loadingProgress.visibility = View.GONE

                // Enable All Buttons
                disableAllButtons(false)
            }
        } else {

            Log.e(LOGINACTIVITY_TAG, "googleSignIn: Different Request Code")

            // Notify user about Login Failure
            Toast.makeText(
                applicationContext,
                "Login failed! Error: Different Request Code",
                Toast.LENGTH_SHORT
            ).show()

            // Hide Loading - Hide Progress Bar
            _binding.loadingProgress.visibility = View.GONE

            // Enable All Buttons
            disableAllButtons(false)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        auth = Firebase.auth
        db = Firebase.firestore

        _binding.apply {

            // Email Login
            emailLoginBtn.setOnClickListener {

                // Show Loading - Hide Progress Bar
                _binding.loadingProgress.visibility = View.VISIBLE

                // Disable all buttons while login in progress
                disableAllButtons(true)

                val email = emailAddressEdt.text.trim().toString()
                val password = passwordEdt.text.trim().toString()

                val isValid = validateSignInFields(email, password)

                if (isValid) {
                    loginUserWithEmailAndPassword(email, password)
                }
            }

            // Google Login
            googleSignInBtn.setOnClickListener {

                // Show Loading - Hide Progress Bar
                _binding.loadingProgress.visibility = View.VISIBLE

                // Disable all buttons while login in progress
                disableAllButtons(true)

                // Login with Google
                loginWithGoogle()
            }

            signUpBtn.setOnClickListener {
                // Navigate to SignUpActivity
                navigateToSignUpActivity()
            }

        }

    }

    private fun loginWithGoogle() {

        // Step 1: Create Google Sign-In Options
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(resources.getString(R.string.web_client_id))
            .requestEmail()
            .build()

        // Step 2: Declare and initialize GoogleSignInClient
        googleSignInClient = GoogleSignIn.getClient(this@MainActivity, googleSignInOptions)

        // Step 3: Create an SignIn Intent and StartActivityForResult
        try {

            // To clear the saved session of the user in the App Cache for account chooser to appear
            googleSignInClient.signOut()

            val intent = googleSignInClient.signInIntent
            googleSignInResultLauncher.launch(intent)

        } catch (e: Exception) {

            Log.e(LOGINACTIVITY_TAG, "googleSignIn: Error: $e")

            // Notify user about Login Failure
            Toast.makeText(
                applicationContext,
                "Login failed! Error: $e",
                Toast.LENGTH_SHORT
            ).show()

            // Hide Loading - Hide Progress Bar
            _binding.loadingProgress.visibility = View.GONE

            // Enable All Buttons
            disableAllButtons(false)
        }
    }

    private fun signInUserWithGoogleIDToken(idToken: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            if (idToken != null) {

                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

                auth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Notify user about Login Success
                            Toast.makeText(
                                applicationContext,
                                "Login Successful",
                                Toast.LENGTH_SHORT
                            ).show()

                            val currentUser = auth.currentUser
                            if (currentUser != null) {
                                checkIfUserDataAlreadyAvailable(currentUser)
                            }
                        } else {

                            Log.e(
                                LOGINACTIVITY_TAG,
                                "googleSignIn: Sign In Failed. No ID Token found!!! ${task.exception}"
                            )

                            // Notify user about Login Failure
                            Toast.makeText(
                                applicationContext,
                                "Login failed! Error: No ID Token found!!! ${task.exception}",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Hide Loading - Hide Progress Bar
                            _binding.loadingProgress.visibility = View.GONE

                            // Enable All Buttons
                            disableAllButtons(false)

                        }
                    }

            }
        }
    }

    private fun checkIfUserDataAlreadyAvailable(currentUser: FirebaseUser) {

        CoroutineScope(Dispatchers.IO).launch {
            db.collection(Constants.USER_COLLECTION)
                .document(currentUser.uid)
                .get()
                .addOnCompleteListener { task ->

                    if (task.isSuccessful) {
                        val isAlreadyAvailable = task.result.exists()

                        if (!isAlreadyAvailable) {
                            val uID = currentUser.uid
                            val userEmail = currentUser.email
                            val userFullName = currentUser.displayName

                            if (userEmail != null && userFullName != null) {
                                val userInfo = UserInfo(
                                    fullName = userFullName,
                                    email = userEmail,
                                    uId = uID
                                )

                                saveUserDetailsToDB(userInfo)
                            }
                        } else {
                            // Navigate to Home Activity
                            navigateToHomeActivity()
                        }
                    } else {

                        // Notify the user about login failure
                        Toast.makeText(
                            applicationContext,
                            "Login Failed. Error: ${task.exception}",
                            Toast.LENGTH_LONG
                        ).show()

                        Log.e(LOGINACTIVITY_TAG, "loginUser: ${task.exception}")

                        // Hide Loading - Hide Progress Bar
                        _binding.loadingProgress.visibility = View.GONE

                        // Enable All Buttons
                        disableAllButtons(false)
                    }
                }
        }
    }

    private fun saveUserDetailsToDB(userInfo: UserInfo) {

        Log.i(LOGINACTIVITY_TAG, "saveUserDetailsToDB: saveUserDetailsToDB() Called")

        CoroutineScope(Dispatchers.IO).launch {
            db.collection(Constants.USER_COLLECTION)
                .document(userInfo.uId)
                .set(userInfo)
                .addOnSuccessListener {
                    Log.i(
                        LOGINACTIVITY_TAG,
                        "saveUserDetailsToDB: Successfully saved User to DB"
                    )

                    // Navigate to Home Activity
                    navigateToHomeActivity()

                    // Hide Loading - Hide Progress Bar
                    _binding.loadingProgress.visibility = View.GONE

                    // Enable All Buttons
                    disableAllButtons(false)

                }
                .addOnFailureListener { exception ->
                    Log.i(
                        LOGINACTIVITY_TAG,
                        "saveUserDetailsToDB: Error: $exception"
                    )

                    Toast.makeText(
                        applicationContext,
                        "Error: $exception",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Hide Loading - Hide Progress Bar
                    _binding.loadingProgress.visibility = View.GONE

                    // Enable All Buttons
                    disableAllButtons(false)
                }
        }
    }

    private fun disableAllButtons(disable: Boolean) {

        // disable = true - Disable all buttons
        // disable = false - Enable all buttons

        _binding.apply {
            emailLoginBtn.isEnabled = !disable
            googleSignInBtn.isEnabled = !disable
            signUpBtn.isEnabled = !disable
        }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            navigateToHomeActivity()
        }
    }

    private fun validateSignInFields(
        email: String,
        password: String
    ): Boolean {

        // Check if any of the field is empty
        if (email.isNotEmpty() && password.isNotEmpty()) {
            return true
        } else {
            Toast.makeText(
                applicationContext,
                "Passwords do not match!",
                Toast.LENGTH_SHORT
            ).show()

            // Hide Loading - Hide Progress Bar
            _binding.loadingProgress.visibility = View.GONE

            // Enable All Buttons
            disableAllButtons(false)

        }

        return false
    }

    private fun loginUserWithEmailAndPassword(email: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            auth
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->

                    disableAllButtons(false)

                    // Stop Loading - Hide Progress Bar
                    _binding.loadingProgress.visibility = View.GONE

                    if (task.isSuccessful) {

                        Toast.makeText(
                            applicationContext,
                            "Login Successful :)",
                            Toast.LENGTH_SHORT
                        ).show()

                        navigateToHomeActivity()

                    } else {

                        Log.i(
                            SIGNUPACTIVITY_TAG,
                            "loginUserWithEmailAndPassword: Error: ${task.exception}"
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

    private fun navigateToHomeActivity() {
        val intent = Intent(this@MainActivity, HomeActivity::class.java)
        startActivity(intent)

        // Clear Back Stack
        finishAffinity()
    }

    private fun navigateToSignUpActivity() {
        val intent = Intent(this@MainActivity, SignUpActivity::class.java)
        startActivity(intent)
    }
}