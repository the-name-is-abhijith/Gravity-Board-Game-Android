package com.voiezstudio.gravity.activities

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.voiezstudio.gravity.R
import com.voiezstudio.gravity.model.Table
import com.voiezstudio.gravity.util.MyService
import com.voiezstudio.gravity.util.NetworkConnection
import java.io.Serializable
import kotlinx.android.synthetic.main.activity_main.*

class ViewBoardActivity : AppCompatActivity() {

    private lateinit var btnClose: Button
    private lateinit var txtplayer1: TextView
    private lateinit var txtplayer2: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_board)

        val networkConnection = NetworkConnection(applicationContext)
        networkConnection.observe(this, { isConnected ->

            if (!isConnected) {

                val intent = Intent(this, NoNetworkActivity::class.java)
                intent.putExtra("activity", this.toString())
                startActivity(intent)
            }
        })

        startService(Intent(this, MyService::class.java))

        btnClose = findViewById(R.id.btnClose)
        val buttons: Array<Array<Button>> = arrayOf(
            arrayOf(btn00, btn01, btn02, btn03, btn04, btn05, btn06),
            arrayOf(btn10, btn11, btn12, btn13, btn14, btn15, btn16),
            arrayOf(btn20, btn21, btn22, btn23, btn24, btn25, btn26),
            arrayOf(btn30, btn31, btn32, btn33, btn34, btn35, btn36),
            arrayOf(btn40, btn41, btn42, btn43, btn44, btn45, btn46),
            arrayOf(btn50, btn51, btn52, btn53, btn54, btn55, btn56),
            arrayOf(btn60, btn61, btn62, btn63, btn64, btn65, btn66)
        )
        txtplayer1 = findViewById(R.id.txtPlayer1)
        txtplayer2 = findViewById(R.id.txtPlayer2)

        val tempTable: Table = intent.getSerializableExtra("table") as Table

        txtplayer1.text = tempTable.player1
        txtplayer2.text = tempTable.player2

        for (i in 0 until 7) {
            for (j in 0 until 7) {

                val btnId = "btn$i$j"
                val resId = resources.getIdentifier(btnId, "id", packageName)
                buttons[i][j] = findViewById(resId)
                buttons[i][j].isClickable = false
                if (tempTable.buttons[i][j] == "host") {

                    buttons[i][j].setBackgroundDrawable(resources.getDrawable(R.drawable.host))
                } else if (tempTable.buttons[i][j] == "guest") {

                    buttons[i][j].setBackgroundDrawable(resources.getDrawable(R.drawable.guest))
                }
            }
        }

        btnClose.setOnClickListener {

            val sharedPref = getSharedPreferences("gravity", Context.MODE_PRIVATE)
            val user = sharedPref.getString("user", "")
            val id = sharedPref.getString("id", "")
            sharedPref.edit().putString("closed", user).apply()
            if (id != "" && user != "") {
                val dref =
                    FirebaseDatabase.getInstance().getReference("two_player/$id/$user" + "active")
                dref.setValue(false).addOnCompleteListener {

                    startActivity(Intent(this, GameStartActivity::class.java))
                    finish()
                }
            }
        }
    }

    override fun onBackPressed() {

    }
}