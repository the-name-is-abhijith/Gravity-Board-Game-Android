package com.voiezstudio.gravity.activities

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.database.FirebaseDatabase
import com.voiezstudio.gravity.R
import com.voiezstudio.gravity.util.NetworkConnection
import kotlin.system.exitProcess

class NoNetworkActivity : AppCompatActivity() {

    private lateinit var btnExit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_no_network)

        val networkConnection = NetworkConnection(applicationContext)
        networkConnection.observe(this, { isConnected ->

            if (isConnected) {

                if (intent.getStringExtra("activity") == "WaitActivity") {

                    val sharedPref = getSharedPreferences("gravity", MODE_PRIVATE)
                    val id = sharedPref.getString("id", "")
                    if (id != "") {

                        val tempRef =
                            FirebaseDatabase.getInstance()
                                .getReference("two_player/$id")
                        tempRef.removeValue()
                        startActivity(
                            Intent(
                                this,
                                GameStartActivity::class.java
                            )
                        )
                        finish()
                        exitProcess(0)
                    }
                } else {

                    super.onBackPressed()
                }
            }
        })
        btnExit = findViewById(R.id.btnExit)

        btnExit.setOnClickListener {

            finishAffinity()
        }
    }

    override fun onBackPressed() {

    }
}