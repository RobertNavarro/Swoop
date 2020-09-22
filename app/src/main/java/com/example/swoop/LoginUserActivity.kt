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
import kotlinx.android.synthetic.main.activity_login.*

class LoginUserActivity : AppCompatActivity()
{

    private lateinit var mAuth: FirebaseAuth
    private val userPreferences = "UserPrefs"
    private lateinit var sharedPreferences : SharedPreferences
    private var userEmail: String = ""
    private var userSchool: String = ""
    private var inDatabase = false

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences(userPreferences, Context.MODE_PRIVATE)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()

        login.setOnClickListener{
            loginUser()
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

    public override fun onStart()
    {
        super.onStart()
        val currentUser = mAuth.currentUser
        //updateUI(currentUser)
    }

    private fun updateUI(currentUser : FirebaseUser?)
    {
        if(currentUser != null)// Check if user is signed in (non-null) and update UI accordingly.
        {
            if(currentUser.isEmailVerified)
            {
                checkIfUserInDatabase(currentUser, object: FirebaseCallback {
                    override fun onCallback(callbackBool:Boolean)
                    {
                        if(!inDatabase)
                        {
                            updateData()
                            return
                        }
                    }
                })

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

    private fun checkIfUserInDatabase(currentUser : FirebaseUser?, firebaseCallback: FirebaseCallback):Boolean
    {
        if(currentUser != null)
        {
            Toast.makeText(baseContext, "The user isn't null", Toast.LENGTH_SHORT).show()
            val userId = currentUser.getUid()
            val currentUserDB: DatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(userId)
            currentUserDB.addListenerForSingleValueEvent(object: ValueEventListener
            {
                override fun onDataChange(dataSnapshot: DataSnapshot)
                {
                    inDatabase = dataSnapshot.exists()
                    Toast.makeText(baseContext, "The dataSnapshot is " + inDatabase, Toast.LENGTH_SHORT).show()
                    firebaseCallback.onCallback(inDatabase)
                }
                override fun onCancelled(databaseError: DatabaseError) {}
            })
        }
        return inDatabase
    }


    private fun updateData()
    {
        val currentUser = FirebaseAuth.getInstance().getCurrentUser()
        if(currentUser != null)
        {
            Toast.makeText(baseContext, "Inside !checkIfUserInDatabase(currentUser)",Toast.LENGTH_LONG).show()
            val userId = currentUser.getUid()
            val currentUserDB: DatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(userId)
            val currentUserEmail: DatabaseReference = currentUserDB.child("User Email")
            val currentUserSchool: DatabaseReference = currentUserDB.child("User School")
            currentUserDB.setValue(true)
            getSchoolFromEmail(userEmail)
            currentUserEmail.setValue(userEmail)
            currentUserSchool.setValue(userSchool)

            startActivity(Intent(this, NameOfUserActivity::class.java))
            finish()
            return
        }
    }

    private interface FirebaseCallback{
        fun onCallback(callbackBool:Boolean)
    }

    private fun getSchoolFromEmail(email: String)
    {
        return when
        {
            email.contains("ucr.edu", ignoreCase = true) ->  {userSchool = "UCR"}
            email.contains("ucla.edu", ignoreCase = true) -> {userSchool = "UCLA"}
            email.contains("ucsc.edu", ignoreCase = true) -> {userSchool = "UCSC"}
            email.contains("ucsd.edu", ignoreCase = true) -> {userSchool = "UCSD"}
            email.contains("ucdavis.edu", ignoreCase = true) -> {userSchool = "UCD"}
            email.contains("uci.edu", ignoreCase = true) -> {userSchool = "UCR"}
            email.contains("ucmerced.edu", ignoreCase = true) -> {userSchool = "UCM"}
            email.contains("ucsb.edu", ignoreCase = true) -> {userSchool = "UCSB"}
            email.contains("berkeley.edu", ignoreCase = true) -> {userSchool = "UCB"}
            email.contains("ucsf.edu", ignoreCase = true) -> {userSchool = "UCSF"}
            else -> {userSchool = ""}
        }
    }

}