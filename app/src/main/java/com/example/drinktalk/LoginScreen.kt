package com.example.drinktalk

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginScreen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Remove all window decorators to make the app truly fullscreen
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()

        val inpNickname: EditText = findViewById(R.id.inpNickname)

        // Hide the keyboard on outside click
        findViewById<View>(android.R.id.content).setOnClickListener{
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }

        // If the nickname is properly set, go to create game menu
        findViewById<Button>(R.id.btnCreateGame).setOnClickListener {
            if (!inpNickname.text.isNullOrEmpty()) {
                val i = Intent(this, CreateGame::class.java).apply {
                    putExtra("nickname", inpNickname.text.toString())
                }

                startActivity(i)
            } else {
                Toast.makeText(this, "Nadimak mora sadržavati barem jedan znak!", Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onStart() {
        super.onStart()

        // Request all the needed permission on activity start
        requestPermissions(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            1)
    }

    // Back button exits activity
    override fun onBackPressed() {
        finish()
    }

}