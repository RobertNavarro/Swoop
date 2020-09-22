package com.example.swoop

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.Button
import android.content.Intent
import android.content.SharedPreferences
import android.util.Patterns
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_login_redirect.*

class DriverLoginActivity : AppCompatActivity() {

    val UserPrefrences = "UserPrefs"
    lateinit var sharedPreferences : SharedPreferences

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences(UserPrefrences, Context.MODE_PRIVATE)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()

//       registration.setOnClickListener{
//            registerUser()
//        }

        login.setOnClickListener{
            loginUser()
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
                    sharedPreferences.edit().putString("role", "Driver").apply()
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
        updateUI(currentUser)
    }

    private fun updateUI(currentUser : FirebaseUser?)
    {
        if(currentUser != null)// Check if user is signed in (non-null) and update UI accordingly.
        {
            if(currentUser.isEmailVerified)
            {

                var userId = currentUser.getUid()
                var currentUserDB: DatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId)
                currentUserDB.setValue(true)
               // setContentView(R.layout.activity_driver_map)
                startActivity(Intent(this, DriverMapActivity::class.java))
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
            email.contains("ucr.edu", ignoreCase = true) ->  true
            email.contains("ucla.edu", ignoreCase = true) -> true
            email.contains("ucsc.edu", ignoreCase = true) -> true
            email.contains("ucsd.edu", ignoreCase = true) -> true
            email.contains("ucdavis.edu", ignoreCase = true) -> true
            email.contains("uci.edu", ignoreCase = true) -> true
            email.contains("ucmerced.edu", ignoreCase = true) -> true
            email.contains("ucsb.edu", ignoreCase = true) -> true
            email.contains("berkeley.edu", ignoreCase = true) -> true
            email.contains("ucsf.edu", ignoreCase = true) -> true
            else -> false
        }
    }

}
