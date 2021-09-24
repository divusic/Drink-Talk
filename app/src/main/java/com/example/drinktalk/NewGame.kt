package com.example.drinktalk

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.drinktalk.data.SpinnerItem
import com.example.drinktalk.util.ConnectionSingleton
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter


class NewGame : AppCompatActivity() {

    var timesList = arrayOf(
        SpinnerItem(1,"30 MIN"), SpinnerItem(2,"1 H"),
        SpinnerItem(3,"1H 30 MIN"), SpinnerItem(4,"2 H"),
        SpinnerItem(5,"2H 30 MIN"), SpinnerItem(6,"3H")
    )

    val playersList = mutableListOf<String>()
    private val connString: String = getConnectionString()
    private lateinit var spin: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_game)

        // Remove all window decorators to make the app truly fullscreen
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()

        val hostNickname = intent.getStringExtra("hostNickname").toString()

        // Spinner and listviews setup
        spin = findViewById(R.id.timeDropDown)
        val playersListView: ListView = findViewById(R.id.playersList)
        val playersListViewExpanded: ListView = findViewById(R.id.playersListExpanded)

        val playersListAdapter =
            ArrayAdapter(this, R.layout.custom_listview, R.id.playersNickname, playersList)

        val spinAdapter = ArrayAdapter(this, R.layout.custom_spinner_item, timesList)

        spinAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item)

        spin.adapter = spinAdapter

        playersListView.isEnabled = false
        playersListViewExpanded.isEnabled = false

        playersListView.adapter = playersListAdapter
        playersListViewExpanded.adapter = playersListAdapter

        // Add the host to the player list
        playersList.add((playersList.size + 1).toString() + ". " + hostNickname)
        playersListAdapter.notifyDataSetChanged()

        // Connection setup

        ConnectionSingleton.setNickname(hostNickname)
        ConnectionSingleton.setHost(true)
        ConnectionSingleton.setConnectionHash(connString)

        ConnectionSingleton.startAdvertising()
        ConnectionSingleton.addListener(object: ConnectionSingleton.Listener {
            override fun onResponse(endpointId: String, message: String) {
            }

            override fun onConnected(endpointId: String, nickname: String) {
                // If a new client endpoint is connected, add the player to the player list
                playersList.add((playersList.size + 1).toString() + ". " + nickname)
                playersListAdapter.notifyDataSetChanged()
                ConnectionSingleton.sendMessage(endpointId, timesList[spin.selectedItemPosition].id.toString() + "." + spin.selectedItem.toString())
            }

            override fun onDisconnected(endpointId: String, nickname: String) {
                // If a client endpoint disconnected, remove player from player list
                playersList.remove((playersList.size + 1).toString() + ". " + nickname)
                playersListAdapter.notifyDataSetChanged()
            }
        })

        // Generate QR code then put it inside our image view
        findViewById<ImageView>(R.id.qrHolder).setImageBitmap(generateQrCode())

        findViewById<Button>(R.id.btnExpandList).setOnClickListener {
            findViewById<ConstraintLayout>(R.id.newGameLayout).visibility = View.GONE
            findViewById<ConstraintLayout>(R.id.newGameExtendedLayout).visibility = View.VISIBLE
        }

        findViewById<Button>(R.id.btnExitExpList).setOnClickListener {
            findViewById<ConstraintLayout>(R.id.newGameLayout).visibility = View.VISIBLE
            findViewById<ConstraintLayout>(R.id.newGameExtendedLayout).visibility = View.GONE
        }

        findViewById<Button>(R.id.btnStartGame).setOnClickListener {
            finish()
            startGame()
        }

        findViewById<Button>(R.id.btnBck).setOnClickListener {
            ConnectionSingleton.stopAdvertising()
            finish()
        }
    }

    /**
     * Starts the game, leads to countdown before start
     */
    private fun startGame(){
        val i = Intent(this, CountDown::class.java).apply{
            putExtra("countdownTime", spin.selectedItem.toString())
            putExtra("timeId", timesList[spin.selectedItemPosition].id)
        }
        startActivity(i)
    }

    /**
     * Generates QR code based on the connection hash
     */
    private fun generateQrCode(): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(connString, BarcodeFormat.QR_CODE, 1024, 1024)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }

        return bitmap
    }

    /**
     * Generates random 16 character long connection hash
     */
    private fun getConnectionString(): String{
        val charset = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..16)
            .map { charset.random() }
            .joinToString("")
    }

    override fun onBackPressed() {
        ConnectionSingleton.stopAdvertising()
        finish()
    }
}