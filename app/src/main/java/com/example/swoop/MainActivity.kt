package com.example.swoop
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity()
{
    private lateinit var mLogin: Button //= driver
    private lateinit var  mRegister: Button //= customer
    val userPreferences = "UserPrefs"
    lateinit var sharedPreferences : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences(userPreferences, Context.MODE_PRIVATE)

        if(sharedPreferences != null)
        {
            checkIfUserAlreadyLoggedIn()
        }

        setContentView(R.layout.activity_main)
        mLogin = login
        mRegister = register

        mLogin.setOnClickListener {
                val intent = Intent(this, LoginUserActivity::class.java)
                startActivity(intent)
                finish()
        }
        mRegister.setOnClickListener {
            val intent = Intent(this, RegisterUserActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun checkIfUserAlreadyLoggedIn() //checks if the user has logged into their account on their phone already
    {

        if(sharedPreferences.getString("role","") != null && sharedPreferences.getString("role","") != "")
        {
            if(sharedPreferences.getString("role","") == "Driver")
            {
                val intent = Intent(this,DriverMapActivity::class.java)
                startActivity(intent)
                finish()
            }

            if(sharedPreferences.getString("role","") == "Rider")
            {
                val intent = Intent(this, RiderMapActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}