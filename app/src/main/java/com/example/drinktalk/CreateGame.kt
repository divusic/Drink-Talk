package com.example.drinktalk

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.drinktalk.util.ConnectionSingleton
import com.google.android.gms.nearby.Nearby
import com.google.zxing.integration.android.IntentIntegrator

class CreateGame : AppCompatActivity() {

    private var nickname: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_game)

        // Remove all window decorators to make the app truly fullscreen
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()

        // Create new connection client
        ConnectionSingleton.setConnectionClient(Nearby.getConnectionsClient(this))

        nickname = intent.getStringExtra("nickname").toString()

        val txtNickname: TextView = findViewById(R.id.txtNickname)
        txtNickname.text = nickname

        // Connection setup
        ConnectionSingleton.addListener(object: ConnectionSingleton.Listener {
            override fun onResponse(endpointId: String, message: String) {
                if (!ConnectionSingleton.getHost()) {
                    goToStartGame(message)
                }
            }

            override fun onConnected(endpointId: String, nickname: String) {
            }

            override fun onDisconnected(endpointId: String, nickname: String) {
            }
        })

        findViewById<Button>(R.id.btnCreateGame).setOnClickListener {
            createGame()
        }

        findViewById<Button>(R.id.btnExistingGame).setOnClickListener {
            startQrScanner()
        }

        findViewById<Button>(R.id.btnBck).setOnClickListener {
            ConnectionSingleton.stopDiscovery()
            finish()
        }
    }

    private fun createGame(){
        val i = Intent(this, NewGame::class.java).apply {
            putExtra("hostNickname", nickname)
        }

        startActivity(i)
    }

    /**
     * Goes to countdown and waits for host game start message
     */
    private fun goToStartGame(time: String){
        val i = Intent(this, CountDown::class.java).apply {
            putExtra("nickname",nickname)
            putExtra("time", time.split(".")[1])
            putExtra("timeId", time.split(".")[0].toInt())
        }

        startActivity(i)
    }

    /**
     * Starts the QR scanner API to get the connection hash from QR code
     */
    private fun startQrScanner(){
        val intentIntegrator = IntentIntegrator(this)
        intentIntegrator.setBeepEnabled(false)
        intentIntegrator.setCameraId(0)
        intentIntegrator.setPrompt("Skeniraj QR kod igre")
        intentIntegrator.setBarcodeImageEnabled(false)
        intentIntegrator.initiateScan()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "Prekinuto skeniranje QR koda", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("CreateGameActivity", "Scanned")

                Toast.makeText(this, "Započeto traženje host-a.", Toast.LENGTH_LONG).show()

                val btn: Button = findViewById(R.id.btnCreateGame)
                btn.isEnabled = false
                btn.isClickable = false

                // Set up connection data
                ConnectionSingleton.setNickname(nickname)
                ConnectionSingleton.setConnectionHash(result.contents)
                ConnectionSingleton.startDiscovery()

            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
            Log.d("CreateGameActivity", "No data scanned")
        }
    }

    override fun onBackPressed() {
        ConnectionSingleton.stopDiscovery()
        finish()
    }

}