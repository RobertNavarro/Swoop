package com.example.swoop

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_login.*


class SignUpActivityCustomer : AppCompatActivity(){

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?)
    {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        mAuth = FirebaseAuth.getInstance()


//        registration.setOnClickListener {
//
//            registerUser()
//        }
    }
    fun registerUser(){

        if(email.text.toString().isEmpty())
        {
            email.error = "Please enter email."
            email.requestFocus()
            return
        }

        if(!Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches())
        {
            email.error = "Please enter a valid email."
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

                    var currentUser = FirebaseAuth.getInstance().getCurrentUser()
                    if(currentUser != null)
                    {

                        var userId = currentUser.getUid()


                        var currentUserDB: DatabaseReference =
                            FirebaseDatabase.getInstance().getReference().child("Users")
                                .child("Customers").child(userId)
                        currentUserDB.setValue(true)

                        currentUser.sendEmailVerification()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    //startActivity(Intent(this, CustomerLoginActivity::class.java))
                                    finish()
                                }
                            }

                    }

                    // Sign in success, update UI with the signed-in user's information

                } else {
                    Toast.makeText(baseContext, "Failed to sign up. Please try again later.",
                        Toast.LENGTH_SHORT).show()
                }

            }

    }
}