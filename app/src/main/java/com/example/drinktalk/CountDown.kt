package com.example.drinktalk

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.HtmlCompat
import com.example.drinktalk.util.ConnectionSingleton
import java.util.*
import kotlin.math.abs


class CountDown : AppCompatActivity() {

    lateinit var sm: SensorManager
    lateinit var list: MutableList<Sensor>
    lateinit var sel: SensorEventListener
    lateinit var startTime: Date
    lateinit var endTime: Date
    var timeDifferenceSeconds: Float = 0.0F
    var timeLengthCountDown: Float = 0.0F

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_in_progress)

        // Remove all window decorators to make the app truly fullscreen
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()

        // Parse all intent variables
        val intentValueString = intent.getStringExtra("countdownTime").toString()
        val time = intent.getStringExtra("time")
        val timeId = intent.getIntExtra("timeId", 0)

        // Calculate set time for the game length
        timeLengthCountDown = timeId * 30.0F * 60.0F

        val timerTime = if(!time.isNullOrEmpty()) time.toString() else intentValueString

        var startZ = 0.0F
        var finishZ = 0.0F
        val textMain = findViewById<TextView>(R.id.textMain)
        val textMainGameStarted = findViewById<TextView>(R.id.textMainGameStarted)

        // Stop discovery and advertising if the game has started
        if (ConnectionSingleton.getHost()) {
            ConnectionSingleton.stopAdvertising()
        } else {
            ConnectionSingleton.stopDiscovery()
        }

        // Setup for the main game screen text views
        // Used html text tags to get bold text and insert selected time
        textMain.text = HtmlCompat.fromHtml(getString(R.string.msgMain, timerTime), HtmlCompat.FROM_HTML_MODE_COMPACT)
        textMainGameStarted.text = HtmlCompat.fromHtml(getString(R.string.msgMain, timerTime), HtmlCompat.FROM_HTML_MODE_COMPACT)

        // Host needs to broadcast start of the game to all connected client endpoints
        if(ConnectionSingleton.getHost()){
            ConnectionSingleton.sendToAll("start")
            startCountdown()
        }

        ConnectionSingleton.addListener(object: ConnectionSingleton.Listener {
            override fun onResponse(endpointId: String, message: String) {
                // Start the game on connected client endpoints that received the message
                if (!ConnectionSingleton.getHost() && message == "start") {
                    startCountdown()
                }

                if (ConnectionSingleton.getHost() && message.startsWith("loser:")){
                    ConnectionSingleton.sendToAll(message)
                    goToResult(false, message.split(":")[1])
                } else if (ConnectionSingleton.getHost() && message.startsWith("winner")){
                    ConnectionSingleton.sendToAll(message)
                    goToResult(true, "")
                }

                if (!ConnectionSingleton.getHost() && message.startsWith("loser:")){
                    goToResult(false, message.split(":")[1])
                } else if (!ConnectionSingleton.getHost() && message.startsWith("winner")){
                    goToResult(true, "")
                }
            }

            override fun onConnected(endpointId: String, nickname: String) {
            }

            override fun onDisconnected(endpointId: String, nickname: String) {
            }
        })

        // Variables needed for phone movement sensors / accelerometer
        sm = getSystemService(SENSOR_SERVICE) as SensorManager
        list  = sm.getSensorList(Sensor.TYPE_ACCELEROMETER)

        sel = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            override fun onSensorChanged(event: SensorEvent) {
                val values = event.values

                // If the phone has been lifted up
                if (startZ != 0.0F && finishZ != 0.0F && abs(startZ - finishZ) > 2){

                    endTime = Calendar.getInstance().time

                    val timeDifference = endTime.time - startTime.time

                    timeDifferenceSeconds = timeDifference / 1000.0F

                    // Check time difference to determine if the players have won or lost the game
                    if(ConnectionSingleton.getHost()){
                        ConnectionSingleton.sendToAll((if (timeDifferenceSeconds < timeLengthCountDown) "loser:" else "winner") + ConnectionSingleton.getNickname())
                        goToResult(timeDifferenceSeconds > timeLengthCountDown, ConnectionSingleton.getNickname())
                    } else {
                        ConnectionSingleton.sendMessage(ConnectionSingleton.getRemoteEndPoints()[0], (if (timeDifferenceSeconds < timeLengthCountDown) "loser:" else "winner") + ConnectionSingleton.getNickname())
                    }

                    // Stop polling sensors / accelerometer
                    unregisterListeners()
                }

                // Set former and current Z-axis values
                startZ = finishZ
                finishZ = values[2]
            }
        }
    }

    /**
     * Starts the 10 second timer before the game begins
     */
    private fun startCountdown(){
        object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                findViewById<TextView>(R.id.countdownNum).text = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {
                findViewById<ConstraintLayout>(R.id.gameInProgressLayout).visibility = View.GONE
                findViewById<ConstraintLayout>(R.id.gameStartedLayout).visibility = View.VISIBLE

                // Register listener for accelerometer after the game started / timer finished
                if (list.size > 0) {
                    sm.registerListener(sel, list[0], SensorManager.SENSOR_DELAY_NORMAL)
                } else {
                    Toast.makeText(baseContext, "Error: No Accelerometer.", Toast.LENGTH_LONG).show()
                }

                startTime = Calendar.getInstance().time

            }
        }.start()
    }

    /**
     * Open win / lose game result activity
     */
    private fun goToResult(win: Boolean, loserName: String){
        val i = Intent(this, GameResult::class.java).apply{
            putExtra("win", win)
            if (!win){
                putExtra("loserName", loserName)
            }
        }
        finish()
        startActivity(i)
    }

    private fun unregisterListeners(){
        if (list.size > 0) {
            sm.unregisterListener(sel)
        }
    }

    override fun onResume() {
        super.onResume()
        if (list.size > 0) {
            sm.registerListener(sel, list[0], SensorManager.SENSOR_DELAY_NORMAL)

        }
    }

    override fun onStop() {
        unregisterListeners()
        super.onStop()
    }

    // Back button needs to be forbidden in this activity
    override fun onBackPressed() {}
}