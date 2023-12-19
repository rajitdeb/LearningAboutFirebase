package com.rajit.firebaseexampleapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.User
import com.google.firebase.firestore.firestore
import com.rajit.firebaseexampleapp.databinding.ActivityAddUserBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddUserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddUserBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        db = Firebase.firestore

        binding.addToFirestoreBtn.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            uploadUserData()
        }

        binding.batchWriteOperationBtn.setOnClickListener { doBatchOperation() }

    }

    private fun doBatchOperation() {

        binding.progressBar.visibility = View.VISIBLE

        val alanWalkerRef = db.collection("users-data").document()
        val alanWalker = hashMapOf(
            "first" to "Alan",
            "last" to "Walker",
            "birthYear" to "1995"
        )

        val adaLovelaceRef = db.collection("users-data").document()
        val adaLovelace = hashMapOf(
            "first" to "Ada",
            "last" to "Lovelace",
            "birthYear" to "1999"
        )

        val johnDoeRef = db.collection("users-data").document()
        val johnDoe = com.rajit.firebaseexampleapp.model.User(
            "John",
            "Doe",
            "2000"
        )

        CoroutineScope(Dispatchers.IO).launch {
            db.runBatch { batch ->

                // add user alan walker to Firestore
                batch.set(alanWalkerRef, alanWalker)

                // add user ada lovelace to Firestore
                batch.set(adaLovelaceRef, adaLovelace)

                // add user john doe to Firestore
                batch.set(johnDoeRef, johnDoe)

            }.addOnCompleteListener { task ->

                binding.progressBar.visibility = View.GONE

                if(task.isSuccessful) {
                    Toast.makeText(applicationContext, "Data saved Successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

    }

    private fun uploadUserData() {

        binding.apply {

            val fullName = fullNameEdtLayout.editText?.text.toString()
            val splitName = splitFullNameToFirstAndLastName(fullName)

            val firstName = splitName[0]
            val lastName = splitName[1]

            val yearOfBirth = yearOfBirthEdtLayout.editText?.text.toString()

            val user = hashMapOf(
                "first" to firstName,
                "last" to lastName,
                "birthYear" to yearOfBirth
            )

            CoroutineScope(Dispatchers.IO).launch {
                db.collection("users-data")
                    .add(user)
                    .addOnCompleteListener { task ->

                        progressBar.visibility = View.GONE

                        if(task.isSuccessful) {
                            Toast.makeText(applicationContext, "Data saved Successfully", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
            }

        }

    }

    override fun onStart() {
        super.onStart()
        if(auth.currentUser == null) {
            startActivity(Intent(this@AddUserActivity, MainActivity::class.java))
        }
    }

    private fun splitFullNameToFirstAndLastName(fullName: String): ArrayList<String> {
        val nameSplit = fullName.split(" ", ignoreCase = true)

        return if(nameSplit.size == 2) {
            arrayListOf(nameSplit[0], nameSplit[1])
        } else {
            arrayListOf(nameSplit[0], nameSplit[nameSplit.size - 1])
        }
    }
}