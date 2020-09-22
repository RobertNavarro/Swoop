package com.example.swoop

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.android.synthetic.main.activity_login_redirect.*

class RegisterUserActivity : AppCompatActivity()
{

    private lateinit var mAuth: FirebaseAuth
    val userPreferences = "UserPrefs"
    private lateinit var sharedPreferences : SharedPreferences
    private lateinit var userEmail: String
    private lateinit var userSchool: String
    private lateinit var userFirstName: String
    private lateinit var userLastName: String

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences(userPreferences, Context.MODE_PRIVATE)
        setContentView(R.layout.activity_register)

        mAuth = FirebaseAuth.getInstance()

        registration.setOnClickListener{
            registerUser()
          }

    }

    private fun registerUser(){
        mAuth = FirebaseAuth.getInstance()
        if(email.text.toString().isEmpty())
        {
            email.error = "Please enter a valid UC email."
            email.requestFocus()
            return
        }

        if(!Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches())
        {
            email.error = "Please enter a valid UC email."
            email.requestFocus()
            return
        }

        if(!checkIfValidSchoolEmail(email.text.toString()))
        {
            email.error = "Please enter a valid UC email."
            email.requestFocus()
            return
        }

        if(password.text.toString().isEmpty())
        {
            password.error = "Please enter password"
            password.requestFocus()
            return
        }

        if(firstName.text.toString().isEmpty())
        {
            password.error = "Please enter your first name"
            password.requestFocus()
            return
        }
        if(firstName.text.toString().isEmpty())
        {
            password.error = "Please enter your last name"
            password.requestFocus()
            return
        }

        mAuth.createUserWithEmailAndPassword(email.text.toString(), password.text.toString())
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    var currentUser = FirebaseAuth.getInstance().currentUser
                    if(currentUser != null)
                    {
                        currentUser.sendEmailVerification()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(baseContext, "A verification email has been sent. Please verify your email and then login.",
                                        Toast.LENGTH_SHORT).show()

                                    userFirstName = firstName.text.toString()
                                    userLastName = lastName.text.toString()
                                    setContentView(R.layout.activity_login_redirect)
                                    emailRedirect.setText(email.text)
                                    passwordRedirect.setText(password.text)
                                    loginRedirect.setOnClickListener{
                                        loginUser()
                                    }
                                }
                            }
                    }

                } else {
                    Toast.makeText(baseContext, "Failed to sign up. Please try again later.",
                        Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }

    }

    private fun loginUser()
    {
        if(email.text.toString().isEmpty())
        {
            email.error = "Please enter email."
            email.requestFocus()
            return
        }

        if(!Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches())
        {
            email.error = "Please enter a valid UC email."
            email.requestFocus()
            return
        }

        if(password.text.toString().isEmpty())
        {
            password.error = "Please enter password"
            password.requestFocus()
            return
        }

        mAuth.signInWithEmailAndPassword(email.text.toString(), password.text.toString())
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    var currentUser = FirebaseAuth.getInstance().getCurrentUser()
                    //sharedPreferences.edit().putString("role", "Customer").apply()
                    userEmail = email.text.toString()
                    updateUI(currentUser)
                    //finish()
                } else {
                    Toast.makeText(baseContext, "Failed to login. Please try again later.",
                        Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }

    }

//    public override fun onStart()
//    {
//        super.onStart()
//        val currentUser = mAuth.currentUser
//        updateUI(currentUser)
//    }

    private fun updateUI(currentUser : FirebaseUser?)
    {
        if(currentUser != null)// Check if user is signed in (non-null) and update UI accordingly.
        {
            if(currentUser.isEmailVerified)
            {
                if(!checkIfUserInDatabase(currentUser))
                {
                    val userId = currentUser.getUid()
                    val currentUserDB: DatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(userId)
                    val currentUserEmail: DatabaseReference = currentUserDB.child("User Email")
                    val currentUserSchool: DatabaseReference = currentUserDB.child("User School")
                    val currentUserFirstName: DatabaseReference = currentUserDB.child("User First Name")
                    val currentUserLastName: DatabaseReference = currentUserDB.child("User Last Name")
                    currentUserDB.setValue(true)
                    currentUserEmail.setValue(userEmail)
                    currentUserSchool.setValue(userSchool)
                    currentUserFirstName.setValue(userFirstName)
                    currentUserLastName.setValue(userLastName)
                }
                startActivity(Intent(this, RoleSelectActivity::class.java))
                finish()
                return
            }
            else
            {
                Toast.makeText(baseContext, "Please verify your email and try again.",
                    Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun checkIfValidSchoolEmail(email: String):Boolean
    {
        return when
        {
            email.contains("ucr.edu", ignoreCase = true) ->  {userSchool = "UCR"; true}
            email.contains("ucla.edu", ignoreCase = true) -> {userSchool = "UCLA"; true}
            email.contains("ucsc.edu", ignoreCase = true) -> {userSchool = "UCSC"; true}
            email.contains("ucsd.edu", ignoreCase = true) -> {userSchool = "UCSD"; true}
            email.contains("ucdavis.edu", ignoreCase = true) -> {userSchool = "UCD"; true}
            email.contains("uci.edu", ignoreCase = true) -> {userSchool = "UCR"; true}
            email.contains("ucmerced.edu", ignoreCase = true) -> {userSchool = "UCM"; true}
            email.contains("ucsb.edu", ignoreCase = true) -> {userSchool = "UCSB"; true}
            email.contains("berkeley.edu", ignoreCase = true) -> {userSchool = "UCB"; true}
            email.contains("ucsf.edu", ignoreCase = true) -> {userSchool = "UCSF"; true}
            else -> false
        }
    }

    private fun checkIfUserInDatabase(currentUser : FirebaseUser?):Boolean
    {
        var inDatabase = false
        if(currentUser != null)
        {
            val userId = currentUser.getUid()
            val currentUserDB: DatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(userId)
            currentUserDB.addListenerForSingleValueEvent(object: ValueEventListener
            {
                override fun onDataChange(dataSnapshot: DataSnapshot)
                {
                    inDatabase = dataSnapshot.exists()
                }
                override fun onCancelled(databaseError: DatabaseError) {}
            })
        }
        return inDatabase
    }
}