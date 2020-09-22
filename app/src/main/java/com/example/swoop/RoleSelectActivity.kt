package com.example.swoop
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.content.SharedPreferences
import android.util.Patterns
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_role_select.*

class RoleSelectActivity : AppCompatActivity()
{
    private lateinit var mAuth: FirebaseAuth
    private val userPreferences = "UserPrefs"
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences(userPreferences, Context.MODE_PRIVATE)
        setContentView(R.layout.activity_role_select)

        mAuth = FirebaseAuth.getInstance()

        rider.setOnClickListener {
            callRiderActivity()
        }

        driver.setOnClickListener {
            callDriverActivity()
        }
    }

    private fun callRiderActivity()
    {
        sharedPreferences.edit().putString("role", "Rider").apply()
        startActivity(Intent(this, RiderMapActivity::class.java))
        finish()
        return
    }

    private fun callDriverActivity()
    {
        sharedPreferences.edit().putString("role", "Driver").apply()
        startActivity(Intent(this, DriverMapActivity::class.java))
        finish()
        return
    }
}