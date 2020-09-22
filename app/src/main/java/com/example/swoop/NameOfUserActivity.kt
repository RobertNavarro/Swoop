package com.example.swoop

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.content.SharedPreferences
import android.util.Patterns
import android.widget.Button
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_ask_for_name.*

class NameOfUserActivity : AppCompatActivity()
{
    private lateinit var mAuth: FirebaseAuth
    private var userFirstName: String = ""
    private var userLastName: String = ""

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ask_for_name)

        mAuth = FirebaseAuth.getInstance()
        Toast.makeText(baseContext, "Inside NameOfUserActivity", Toast.LENGTH_SHORT).show()
        continueButton.setOnClickListener{
            askForName()
        }
    }

    private fun askForName()
    {
        if(firstName.text.toString().isEmpty())
        {
            firstName.error = "Please enter your first name"
            firstName.requestFocus()
            return
        }
        if(lastName.text.toString().isEmpty())
        {
            lastName.error = "Please enter your last name"
            lastName.requestFocus()
            return
        }
        var currentUser = FirebaseAuth.getInstance().getCurrentUser()
        if(currentUser != null)
        {
            val userId = currentUser.getUid()
            val currentUserDB: DatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(userId)
            val currentUserFirstName: DatabaseReference = currentUserDB.child("User First Name")
            val currentUserLastName: DatabaseReference = currentUserDB.child("User Last Name")
            userFirstName = firstName.text.toString()
            userLastName = lastName.text.toString()
            currentUserFirstName.setValue(userFirstName)
            currentUserLastName.setValue(userLastName)
            finish()
        }
    }
}