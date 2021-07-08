package com.voiezstudio.gravity.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.database.*
import com.voiezstudio.gravity.R
import com.voiezstudio.gravity.model.Table
import com.voiezstudio.gravity.util.MyService
import com.voiezstudio.gravity.util.NetworkConnection
import kotlin.system.exitProcess

class WaitActivity : AppCompatActivity() {

    private lateinit var waitLayout: ConstraintLayout
    private lateinit var txtTimer: TextView
    private lateinit var txtRoomCode: TextView
    private lateinit var btnShare: Button
    private lateinit var btnBack: Button
    private lateinit var btnCopy: Button
    private lateinit var ref: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wait)

        startService(Intent(this, MyService::class.java))

        val createdId = intent.getStringExtra("id")!!

        val networkConnection = NetworkConnection(applicationContext)
        networkConnection.observe(this, { isConnected ->

            if (!isConnected) {

                val intent = Intent(this, NoNetworkActivity::class.java)
                intent.putExtra("activity", "WaitActivity")
                startActivity(intent)
                finish()
                exitProcess(0)
            }
        })

        txtRoomCode = findViewById(R.id.txtRoomCode)
        btnShare = findViewById(R.id.btnShare)
        waitLayout = findViewById(R.id.ltProgressLayout)
        txtTimer = findViewById(R.id.txtTimer)
        btnBack = findViewById(R.id.btnBack)
        btnCopy = findViewById(R.id.btnCopy)

        txtRoomCode.text = "Room Code: $createdId"
        ref = FirebaseDatabase.getInstance().getReference("two_player/$createdId")
        var tempTable = Table()
        ref.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                tempTable = snapshot.getValue(Table::class.java)!!

                val c = object : CountDownTimer(180000, 1000) {

                    override fun onTick(millisUntilFinished: Long) {

                        if (tempTable.player2 == "") {

                            val sec = String.format("%02d", (millisUntilFinished / 1000) % 60)
                            val min = String.format("%02d", millisUntilFinished / 60000)
                            txtTimer.text = "$min:$sec"
                        } else {

                            cancel()
                            val intent = Intent(this@WaitActivity, MainActivity::class.java)
                            intent.putExtra("user", "host")
                            intent.putExtra("id", createdId)
                            startActivity(intent)
                            finish()
                            exitProcess(0)
                        }
                    }

                    override fun onFinish() {
                        val tempRef =
                            FirebaseDatabase.getInstance()
                                .getReference("two_player/$createdId")
                        tempRef.removeValue()
                        startActivity(
                            Intent(
                                this@WaitActivity,
                                GameStartActivity::class.java
                            )
                        )
                        finishAffinity()
                    }
                }
                c.start()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    baseContext, error.toString(),
                    Toast.LENGTH_SHORT
                ).show()
            }

        })
        btnShare.setOnClickListener {

            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, createdId)
            startActivity(Intent.createChooser(shareIntent, "Share Using"))
        }
        btnBack.setOnClickListener {

            val dialog = AlertDialog.Builder(this)
            dialog.setTitle("Confirmation")
            dialog.setMessage("Are you sure you want to cancel the room?")
            dialog.setPositiveButton("Yes") { text, listener ->

                val ref = FirebaseDatabase.getInstance().getReference("two_player/$createdId")
                ref.removeValue()
                startActivity(Intent(this@WaitActivity, GameStartActivity::class.java))
                finish()
            }
            dialog.setNegativeButton("No") { text, listener ->

            }
            dialog.create()
            dialog.show()
        }

        btnCopy.setOnClickListener {

            val clipData = ClipData.newPlainText("code", createdId)
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(clipData)
            Toast.makeText(this, "Room code Copied to Clipboard!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {

    }
}