package com.example.drinktalk

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.drinktalk.util.ConnectionSingleton

class GameResult : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_result)

        // Remove all window decorators to make the app truly fullscreen
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()

        // If victory true is received (loserName is null), if lose false
        val victoryBoolean = intent.getBooleanExtra("win", false)
        val loserName = intent.getStringExtra("loserName").toString()

        // Show win / lose screen depending on the game outcome
        if (victoryBoolean){
            findViewById<ConstraintLayout>(R.id.winLayout).visibility = View.VISIBLE
        } else {
            findViewById<TextView>(R.id.loser).text = loserName
            findViewById<ConstraintLayout>(R.id.lossLayout).visibility = View.VISIBLE
        }

        // Cleanup connection and go to create game menu
        findViewById<Button>(R.id.btnNewGame).setOnClickListener {
            ConnectionSingleton.close()

            val i = Intent(this, CreateGame::class.java).apply {
                putExtra("nickname", ConnectionSingleton.getNickname())
            }

            finish()
            startActivity(i)
        }
    }

    // Back button needs to be forbidden in this activity
    override fun onBackPressed() {}
}