package com.voiezstudio.gravity.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.database.FirebaseDatabase
import com.voiezstudio.gravity.R
import com.voiezstudio.gravity.util.MyService
import com.voiezstudio.gravity.util.NetworkConnection

class WinOrLoseActivity : AppCompatActivity() {

    private lateinit var txtWinOrLose: TextView

    lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_win_or_lose)

        val networkConnection = NetworkConnection(applicationContext)
        networkConnection.observe(this, { isConnected ->

            if (!isConnected) {

                val intent = Intent(this, NoNetworkActivity::class.java)
                intent.putExtra("activity", this.toString())
                startActivity(intent)
            }
        })

        startService(Intent(this, MyService::class.java))

        btnBack = findViewById(R.id.btnBack)
        txtWinOrLose = findViewById(R.id.txtWinOrLose)

        val string = intent.getStringExtra("winorlose")

        if (string == "closed") {

            txtWinOrLose.textSize = 18F
            txtWinOrLose.text = "Your Opponent Has Left the Game"
        } else if (string == "DRAW") {

            txtWinOrLose.text = "GAME DRAW!!"
        }
        else{
            txtWinOrLose.text = ("YOU $string")
        }

        btnBack.setOnClickListener {

            if (string == "closed") {
                val sharedPref = getSharedPreferences("gravity", Context.MODE_PRIVATE)
                val id = sharedPref.getString("id", "")
                val ref = FirebaseDatabase.getInstance().getReference("two_player/$id")
                ref.removeValue().addOnSuccessListener {

                    startActivity(Intent(this, GameStartActivity::class.java))
                    finish()
                }
            } else {
                val table = intent.getSerializableExtra("table")
                val newIntent = Intent(this, ViewBoardActivity::class.java)
                newIntent.putExtra("table", table)
                startActivity(newIntent)
                finish()
            }
        }
    }

    override fun onBackPressed() {

    }
}