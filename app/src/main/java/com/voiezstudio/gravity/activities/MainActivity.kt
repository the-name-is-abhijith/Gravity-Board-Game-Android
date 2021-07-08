package com.voiezstudio.gravity.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.drawable.toDrawable
import com.google.firebase.database.*
import com.voiezstudio.gravity.R
import com.voiezstudio.gravity.model.Table
import com.voiezstudio.gravity.util.MyService
import com.voiezstudio.gravity.util.NetworkConnection
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    private lateinit var id: String
    private lateinit var clBoard: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var txtTurn: TextView
    private lateinit var txtPlayer1: TextView
    private lateinit var txtPlayer2: TextView
    private lateinit var btnClose: Button
    var lastTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val networkConnection = NetworkConnection(applicationContext)
        networkConnection.observe(this, { isConnected ->

            if (!isConnected) {

                val intent = Intent(this, NoNetworkActivity::class.java)
                intent.putExtra("activity", this.toString())
                startActivity(intent)
            }
        })

        startService(Intent(this, MyService::class.java))

        val buttons: Array<Array<Button>> = arrayOf(
            arrayOf(btn00, btn01, btn02, btn03, btn04, btn05, btn06),
            arrayOf(btn10, btn11, btn12, btn13, btn14, btn15, btn16),
            arrayOf(btn20, btn21, btn22, btn23, btn24, btn25, btn26),
            arrayOf(btn30, btn31, btn32, btn33, btn34, btn35, btn36),
            arrayOf(btn40, btn41, btn42, btn43, btn44, btn45, btn46),
            arrayOf(btn50, btn51, btn52, btn53, btn54, btn55, btn56),
            arrayOf(btn60, btn61, btn62, btn63, btn64, btn65, btn66)
        )

        id = intent.getStringExtra("id")!!

        clBoard = findViewById(R.id.clBoard)
        txtTurn = findViewById(R.id.txtTurn)
        txtPlayer1 = findViewById(R.id.txtPlayer1)
        txtPlayer2 = findViewById(R.id.txtPlayer2)
        btnClose = findViewById(R.id.btnClose)

        for (i in 0 until 7) {
            for (j in 0 until 7) {

                val btnId = "btn$i$j"
                val resId = resources.getIdentifier(btnId, "id", packageName)
                buttons[i][j] = findViewById(resId)
                buttons[i][j].isClickable = (i == 6)
            }
        }
        val ref = FirebaseDatabase.getInstance().getReference("two_player/$id")
        var tempTable = Table()
        val sharedPref = getSharedPreferences("gravity", Context.MODE_PRIVATE)
        ref.addValueEventListener(
            object : ValueEventListener {

                var finished = false
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        baseContext, error.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onDataChange(snapshot: DataSnapshot) {

                    tempTable = snapshot.getValue(Table::class.java)!!

                    if (!(tempTable.hostactive && tempTable.guestactive) && tempTable.player2 != "" && tempTable.winner == "" && !finished) {

                        val newIntent = Intent(this@MainActivity, WinOrLoseActivity::class.java)
                        if (intent.getStringExtra("user") != sharedPref.getString("closed", "")) {
                            newIntent.putExtra("winorlose", "closed")
                            startActivity(newIntent)
                            finish()
                        }
                    } else if (tempTable.winner != "" && !finished) {
                        finished = true
                        val intentNew = Intent(this@MainActivity, WinOrLoseActivity::class.java)
                        if (tempTable.winner == intent.getStringExtra("user"))
                            intentNew.putExtra("winorlose", "WIN!!")
                        else if (tempTable.winner == "draw")
                            intentNew.putExtra("winorlose","DRAW")
                        else
                            intentNew.putExtra("winorlose", "LOSE!!")
                        intentNew.putExtra("table", tempTable)
                        startActivity(intentNew)
                        finish()
                        exitProcess(0)

                    } else if (!winner(tempTable)) {

                        var count = 0
                        for (i in 0 until 7) {
                            for (j in 0 until 7) {

                                if (buttons[i][j].background != R.drawable.guest.toDrawable() && buttons[i][j].background != R.drawable.host.toDrawable()) {

                                    count = count +1
                                }
                                if (buttons[i][j].background == R.drawable.guest.toDrawable() || buttons[i][j].background == R.drawable.host.toDrawable()) {

                                    buttons[i][j].isClickable = false
                                } else if (i != 6) {

                                    if (tempTable.turn == intent.getStringExtra("user")) {

                                        buttons[i][j].isClickable =
                                            ((tempTable.buttons[i][j] == "") && (tempTable.buttons[i + 1][j] != ""))
                                    } else {

                                        buttons[i][j].isClickable = false
                                        buttons[i + 1][j].isClickable = false
                                    }
                                } else {
                                    buttons[i][j].isClickable = true
                                }

                                if (tempTable.buttons[i][j] == "host") {

                                    buttons[i][j].setBackgroundDrawable(resources.getDrawable(R.drawable.host))
                                } else if (tempTable.buttons[i][j] == "guest") {

                                    buttons[i][j].setBackgroundDrawable(resources.getDrawable(R.drawable.guest))
                                }
                            }
                        }
                        txtPlayer1.text = tempTable.player1
                        txtPlayer2.text = tempTable.player2

                        if (tempTable.turn == intent.getStringExtra("user")) {

                            txtTurn.text = "Your Turn"
                        } else {

                            txtTurn.text = "Opponent's Turn"
                        }
                        if(count == 0){

                            val hashMap: HashMap<String, Any> = HashMap()
                            hashMap.put("winner", "draw")
                            ref.updateChildren(hashMap)
                        }
                    } else if (!finished) {

                        val hashMap: HashMap<String, Any> = HashMap()
                        if (tempTable.turn == "host")
                            hashMap.put("winner", "host")
                        else
                            hashMap.put("winner", "guest")
                        ref.updateChildren(hashMap)
                    }
                }
            })
        for (i in 0 until 7) {
            for (j in 0 until 7) {

                val btn = buttons[i][j]
                btn.setOnClickListener {

                    if (SystemClock.elapsedRealtime() - lastTime < 1000) {
                        return@setOnClickListener
                    }
                    lastTime = SystemClock.elapsedRealtime()
                    for (k in 0 until 7) {
                        for (l in 0 until 7) {
                            if (i == k && j == l) {
                                continue
                            }
                            buttons[k][l].isClickable = false
                        }
                    }
                    if (btn.isClickable && tempTable.turn == intent.getStringExtra("user")) {

                        if (intent.getStringExtra("user") == "host") {

                            buttons[i][j].setBackgroundDrawable(resources.getDrawable(R.drawable.host))

                            val hashMap: HashMap<String, Any> = HashMap()
                            hashMap.put("$j", "host")

                            val turnHashMap: HashMap<String, Any> = HashMap()
                            turnHashMap.put("turn", "guest")

                            ref.child("buttons").child("$i").updateChildren(hashMap)
                                .addOnSuccessListener {

                                    ref.updateChildren(turnHashMap)
                                }
                        } else if (intent.getStringExtra("user") == "guest") {

                            buttons[i][j].setBackgroundDrawable(resources.getDrawable(R.drawable.guest))

                            val hashMap: HashMap<String, Any> = HashMap()
                            hashMap.put("$j", "guest")

                            val turnHashMap: HashMap<String, Any> = HashMap()
                            turnHashMap.put("turn", "host")

                            ref.child("buttons").child("$i").updateChildren(hashMap)
                                .addOnSuccessListener {

                                    ref.updateChildren(turnHashMap)
                                }
                        }
                    }
                }
            }
        }
        btnClose.setOnClickListener {

            val dialog = AlertDialog.Builder(this)
            dialog.setTitle("Confirmation")
            dialog.setMessage("Are you sure you want to give up?")
            dialog.setPositiveButton("Give up") { text, listener ->


                val user = sharedPref.getString("user", "")
                sharedPref.edit().putString("closed", user).apply()
                if (id != "" && user != "") {
                    val dref = FirebaseDatabase.getInstance()
                        .getReference("two_player/$id/$user" + "active")
                    dref.setValue(false).addOnCompleteListener {

                        startActivity(Intent(this@MainActivity, GameStartActivity::class.java))
                        exitProcess(0)
                    }
                }
            }
            dialog.setNegativeButton("Never") { text, listener ->

            }
            dialog.create()
            dialog.show()
        }
    }

    override fun onBackPressed() {
        Toast.makeText(
            baseContext, "You cannot go back at this stage!",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun winner(tempTable: Table): Boolean {

        if (tempTable.buttons[0][0] == tempTable.buttons[0][1] && tempTable.buttons[0][1] == tempTable.buttons[0][2] && tempTable.buttons[0][2] == tempTable.buttons[0][3] && tempTable.buttons[0][0] != "" && tempTable.buttons[0][1] != "" && tempTable.buttons[0][2] != "" && tempTable.buttons[0][3] != "")
            return true
        else if (tempTable.buttons[0][1] == tempTable.buttons[0][2] && tempTable.buttons[0][2] == tempTable.buttons[0][3] && tempTable.buttons[0][3] == tempTable.buttons[0][4] && tempTable.buttons[0][1] != "" && tempTable.buttons[0][2] != "" && tempTable.buttons[0][3] != "" && tempTable.buttons[0][4] != "")
            return true
        else if (tempTable.buttons[0][2] == tempTable.buttons[0][3] && tempTable.buttons[0][3] == tempTable.buttons[0][4] && tempTable.buttons[0][4] == tempTable.buttons[0][5] && tempTable.buttons[0][2] != "" && tempTable.buttons[0][3] != "" && tempTable.buttons[0][4] != "" && tempTable.buttons[0][5] != "")
            return true
        else if (tempTable.buttons[0][3] == tempTable.buttons[0][4] && tempTable.buttons[0][4] == tempTable.buttons[0][5] && tempTable.buttons[0][5] == tempTable.buttons[0][6] && tempTable.buttons[0][3] != "" && tempTable.buttons[0][4] != "" && tempTable.buttons[0][5] != "" && tempTable.buttons[0][6] != "")
            return true
        else if (tempTable.buttons[1][0] == tempTable.buttons[1][1] && tempTable.buttons[1][1] == tempTable.buttons[1][2] && tempTable.buttons[1][2] == tempTable.buttons[1][3] && tempTable.buttons[1][0] != "" && tempTable.buttons[1][1] != "" && tempTable.buttons[1][2] != "" && tempTable.buttons[1][3] != "")
            return true
        else if (tempTable.buttons[1][1] == tempTable.buttons[1][2] && tempTable.buttons[1][2] == tempTable.buttons[1][3] && tempTable.buttons[1][3] == tempTable.buttons[1][4] && tempTable.buttons[1][1] != "" && tempTable.buttons[1][2] != "" && tempTable.buttons[1][3] != "" && tempTable.buttons[1][4] != "")
            return true
        else if (tempTable.buttons[1][2] == tempTable.buttons[1][3] && tempTable.buttons[1][3] == tempTable.buttons[1][4] && tempTable.buttons[1][4] == tempTable.buttons[1][5] && tempTable.buttons[1][2] != "" && tempTable.buttons[1][3] != "" && tempTable.buttons[1][4] != "" && tempTable.buttons[1][5] != "")
            return true
        else if (tempTable.buttons[1][3] == tempTable.buttons[1][4] && tempTable.buttons[1][4] == tempTable.buttons[1][5] && tempTable.buttons[1][5] == tempTable.buttons[1][6] && tempTable.buttons[1][3] != "" && tempTable.buttons[1][4] != "" && tempTable.buttons[1][5] != "" && tempTable.buttons[1][6] != "")
            return true
        else if (tempTable.buttons[2][0] == tempTable.buttons[2][1] && tempTable.buttons[2][1] == tempTable.buttons[2][2] && tempTable.buttons[2][2] == tempTable.buttons[2][3] && tempTable.buttons[2][0] != "" && tempTable.buttons[2][1] != "" && tempTable.buttons[2][2] != "" && tempTable.buttons[2][3] != "")
            return true
        else if (tempTable.buttons[2][1] == tempTable.buttons[2][2] && tempTable.buttons[2][2] == tempTable.buttons[2][3] && tempTable.buttons[2][3] == tempTable.buttons[2][4] && tempTable.buttons[2][1] != "" && tempTable.buttons[2][2] != "" && tempTable.buttons[2][3] != "" && tempTable.buttons[2][4] != "")
            return true
        else if (tempTable.buttons[2][2] == tempTable.buttons[2][3] && tempTable.buttons[2][3] == tempTable.buttons[2][4] && tempTable.buttons[2][4] == tempTable.buttons[2][5] && tempTable.buttons[2][2] != "" && tempTable.buttons[2][3] != "" && tempTable.buttons[2][4] != "" && tempTable.buttons[2][5] != "")
            return true
        else if (tempTable.buttons[2][3] == tempTable.buttons[2][4] && tempTable.buttons[2][4] == tempTable.buttons[2][5] && tempTable.buttons[2][5] == tempTable.buttons[2][6] && tempTable.buttons[2][3] != "" && tempTable.buttons[2][4] != "" && tempTable.buttons[2][5] != "" && tempTable.buttons[2][6] != "")
            return true
        else if (tempTable.buttons[3][0] == tempTable.buttons[3][1] && tempTable.buttons[3][1] == tempTable.buttons[3][2] && tempTable.buttons[3][2] == tempTable.buttons[3][3] && tempTable.buttons[3][0] != "" && tempTable.buttons[3][1] != "" && tempTable.buttons[3][2] != "" && tempTable.buttons[3][3] != "")
            return true
        else if (tempTable.buttons[3][1] == tempTable.buttons[3][2] && tempTable.buttons[3][2] == tempTable.buttons[3][3] && tempTable.buttons[3][3] == tempTable.buttons[3][4] && tempTable.buttons[3][1] != "" && tempTable.buttons[3][2] != "" && tempTable.buttons[3][3] != "" && tempTable.buttons[3][4] != "")
            return true
        else if (tempTable.buttons[3][2] == tempTable.buttons[3][3] && tempTable.buttons[3][3] == tempTable.buttons[3][4] && tempTable.buttons[3][4] == tempTable.buttons[3][5] && tempTable.buttons[3][2] != "" && tempTable.buttons[3][3] != "" && tempTable.buttons[3][4] != "" && tempTable.buttons[3][5] != "")
            return true
        else if (tempTable.buttons[3][3] == tempTable.buttons[3][4] && tempTable.buttons[3][4] == tempTable.buttons[3][5] && tempTable.buttons[3][5] == tempTable.buttons[3][6] && tempTable.buttons[3][3] != "" && tempTable.buttons[3][4] != "" && tempTable.buttons[3][5] != "" && tempTable.buttons[3][6] != "")
            return true
        else if (tempTable.buttons[4][0] == tempTable.buttons[4][1] && tempTable.buttons[4][1] == tempTable.buttons[4][2] && tempTable.buttons[4][2] == tempTable.buttons[4][3] && tempTable.buttons[4][0] != "" && tempTable.buttons[4][1] != "" && tempTable.buttons[4][2] != "" && tempTable.buttons[4][3] != "")
            return true
        else if (tempTable.buttons[4][1] == tempTable.buttons[4][2] && tempTable.buttons[4][2] == tempTable.buttons[4][3] && tempTable.buttons[4][3] == tempTable.buttons[4][4] && tempTable.buttons[4][1] != "" && tempTable.buttons[4][2] != "" && tempTable.buttons[4][3] != "" && tempTable.buttons[4][4] != "")
            return true
        else if (tempTable.buttons[4][2] == tempTable.buttons[4][3] && tempTable.buttons[4][3] == tempTable.buttons[4][4] && tempTable.buttons[4][4] == tempTable.buttons[4][5] && tempTable.buttons[4][2] != "" && tempTable.buttons[4][3] != "" && tempTable.buttons[4][4] != "" && tempTable.buttons[4][5] != "")
            return true
        else if (tempTable.buttons[4][3] == tempTable.buttons[4][4] && tempTable.buttons[4][4] == tempTable.buttons[4][5] && tempTable.buttons[4][5] == tempTable.buttons[4][6] && tempTable.buttons[4][3] != "" && tempTable.buttons[4][4] != "" && tempTable.buttons[4][5] != "" && tempTable.buttons[4][6] != "")
            return true
        else if (tempTable.buttons[5][0] == tempTable.buttons[5][1] && tempTable.buttons[5][1] == tempTable.buttons[5][2] && tempTable.buttons[5][2] == tempTable.buttons[5][3] && tempTable.buttons[5][0] != "" && tempTable.buttons[5][1] != "" && tempTable.buttons[5][2] != "" && tempTable.buttons[5][3] != "")
            return true
        else if (tempTable.buttons[5][1] == tempTable.buttons[5][2] && tempTable.buttons[5][2] == tempTable.buttons[5][3] && tempTable.buttons[5][3] == tempTable.buttons[5][4] && tempTable.buttons[5][1] != "" && tempTable.buttons[5][2] != "" && tempTable.buttons[5][3] != "" && tempTable.buttons[5][4] != "")
            return true
        else if (tempTable.buttons[5][2] == tempTable.buttons[5][3] && tempTable.buttons[5][3] == tempTable.buttons[5][4] && tempTable.buttons[5][4] == tempTable.buttons[5][5] && tempTable.buttons[5][2] != "" && tempTable.buttons[5][3] != "" && tempTable.buttons[5][4] != "" && tempTable.buttons[5][5] != "")
            return true
        else if (tempTable.buttons[5][3] == tempTable.buttons[5][4] && tempTable.buttons[5][4] == tempTable.buttons[5][5] && tempTable.buttons[5][5] == tempTable.buttons[5][6] && tempTable.buttons[5][3] != "" && tempTable.buttons[5][4] != "" && tempTable.buttons[5][5] != "" && tempTable.buttons[5][6] != "")
            return true
        else if (tempTable.buttons[6][0] == tempTable.buttons[6][1] && tempTable.buttons[6][1] == tempTable.buttons[6][2] && tempTable.buttons[6][2] == tempTable.buttons[6][3] && tempTable.buttons[6][0] != "" && tempTable.buttons[6][1] != "" && tempTable.buttons[6][2] != "" && tempTable.buttons[6][3] != "")
            return true
        else if (tempTable.buttons[6][1] == tempTable.buttons[6][2] && tempTable.buttons[6][2] == tempTable.buttons[6][3] && tempTable.buttons[6][3] == tempTable.buttons[6][4] && tempTable.buttons[6][1] != "" && tempTable.buttons[6][2] != "" && tempTable.buttons[6][3] != "" && tempTable.buttons[6][4] != "")
            return true
        else if (tempTable.buttons[6][2] == tempTable.buttons[6][3] && tempTable.buttons[6][3] == tempTable.buttons[6][4] && tempTable.buttons[6][4] == tempTable.buttons[6][5] && tempTable.buttons[6][2] != "" && tempTable.buttons[6][3] != "" && tempTable.buttons[6][4] != "" && tempTable.buttons[6][5] != "")
            return true
        else if (tempTable.buttons[6][3] == tempTable.buttons[6][4] && tempTable.buttons[6][4] == tempTable.buttons[6][5] && tempTable.buttons[6][5] == tempTable.buttons[6][6] && tempTable.buttons[6][3] != "" && tempTable.buttons[6][4] != "" && tempTable.buttons[6][5] != "" && tempTable.buttons[6][6] != "")
            return true
        else if (tempTable.buttons[0][0] == tempTable.buttons[1][0] && tempTable.buttons[1][0] == tempTable.buttons[2][0] && tempTable.buttons[2][0] == tempTable.buttons[3][0] && tempTable.buttons[0][0] != "" && tempTable.buttons[1][0] != "" && tempTable.buttons[2][0] != "" && tempTable.buttons[3][0] != "")
            return true
        else if (tempTable.buttons[1][0] == tempTable.buttons[2][0] && tempTable.buttons[2][0] == tempTable.buttons[3][0] && tempTable.buttons[3][0] == tempTable.buttons[4][0] && tempTable.buttons[1][0] != "" && tempTable.buttons[2][0] != "" && tempTable.buttons[3][0] != "" && tempTable.buttons[4][0] != "")
            return true
        else if (tempTable.buttons[2][0] == tempTable.buttons[3][0] && tempTable.buttons[3][0] == tempTable.buttons[4][0] && tempTable.buttons[4][0] == tempTable.buttons[5][0] && tempTable.buttons[2][0] != "" && tempTable.buttons[3][0] != "" && tempTable.buttons[4][0] != "" && tempTable.buttons[5][0] != "")
            return true
        else if (tempTable.buttons[3][0] == tempTable.buttons[4][0] && tempTable.buttons[4][0] == tempTable.buttons[5][0] && tempTable.buttons[5][0] == tempTable.buttons[6][0] && tempTable.buttons[3][0] != "" && tempTable.buttons[4][0] != "" && tempTable.buttons[5][0] != "" && tempTable.buttons[6][0] != "")
            return true
        else if (tempTable.buttons[0][1] == tempTable.buttons[1][1] && tempTable.buttons[1][1] == tempTable.buttons[2][1] && tempTable.buttons[2][1] == tempTable.buttons[3][1] && tempTable.buttons[0][1] != "" && tempTable.buttons[1][1] != "" && tempTable.buttons[2][1] != "" && tempTable.buttons[3][1] != "")
            return true
        else if (tempTable.buttons[1][1] == tempTable.buttons[2][1] && tempTable.buttons[2][1] == tempTable.buttons[3][1] && tempTable.buttons[3][1] == tempTable.buttons[4][1] && tempTable.buttons[1][1] != "" && tempTable.buttons[2][1] != "" && tempTable.buttons[3][1] != "" && tempTable.buttons[4][1] != "")
            return true
        else if (tempTable.buttons[2][1] == tempTable.buttons[3][1] && tempTable.buttons[3][1] == tempTable.buttons[4][1] && tempTable.buttons[4][1] == tempTable.buttons[5][1] && tempTable.buttons[2][1] != "" && tempTable.buttons[3][1] != "" && tempTable.buttons[4][1] != "" && tempTable.buttons[5][1] != "")
            return true
        else if (tempTable.buttons[3][1] == tempTable.buttons[4][1] && tempTable.buttons[4][1] == tempTable.buttons[5][1] && tempTable.buttons[5][1] == tempTable.buttons[6][1] && tempTable.buttons[3][1] != "" && tempTable.buttons[4][1] != "" && tempTable.buttons[5][1] != "" && tempTable.buttons[6][1] != "")
            return true
        else if (tempTable.buttons[0][2] == tempTable.buttons[1][2] && tempTable.buttons[1][2] == tempTable.buttons[2][2] && tempTable.buttons[2][2] == tempTable.buttons[3][2] && tempTable.buttons[0][2] != "" && tempTable.buttons[1][2] != "" && tempTable.buttons[2][2] != "" && tempTable.buttons[3][2] != "")
            return true
        else if (tempTable.buttons[1][2] == tempTable.buttons[2][2] && tempTable.buttons[2][2] == tempTable.buttons[3][2] && tempTable.buttons[3][2] == tempTable.buttons[4][2] && tempTable.buttons[1][2] != "" && tempTable.buttons[2][2] != "" && tempTable.buttons[3][2] != "" && tempTable.buttons[4][2] != "")
            return true
        else if (tempTable.buttons[2][2] == tempTable.buttons[3][2] && tempTable.buttons[3][2] == tempTable.buttons[4][2] && tempTable.buttons[4][2] == tempTable.buttons[5][2] && tempTable.buttons[2][2] != "" && tempTable.buttons[3][2] != "" && tempTable.buttons[4][2] != "" && tempTable.buttons[5][2] != "")
            return true
        else if (tempTable.buttons[3][2] == tempTable.buttons[4][2] && tempTable.buttons[4][2] == tempTable.buttons[5][2] && tempTable.buttons[5][2] == tempTable.buttons[6][2] && tempTable.buttons[3][2] != "" && tempTable.buttons[4][2] != "" && tempTable.buttons[5][2] != "" && tempTable.buttons[6][2] != "")
            return true
        else if (tempTable.buttons[0][3] == tempTable.buttons[1][3] && tempTable.buttons[1][3] == tempTable.buttons[2][3] && tempTable.buttons[2][3] == tempTable.buttons[3][3] && tempTable.buttons[0][3] != "" && tempTable.buttons[1][3] != "" && tempTable.buttons[2][3] != "" && tempTable.buttons[3][3] != "")
            return true
        else if (tempTable.buttons[1][3] == tempTable.buttons[2][3] && tempTable.buttons[2][3] == tempTable.buttons[3][3] && tempTable.buttons[3][3] == tempTable.buttons[4][3] && tempTable.buttons[1][3] != "" && tempTable.buttons[2][3] != "" && tempTable.buttons[3][3] != "" && tempTable.buttons[4][3] != "" && tempTable.buttons[0][2] != "" && tempTable.buttons[1][2] != "" && tempTable.buttons[2][2] != "" && tempTable.buttons[3][2] != "")
            return true
        else if (tempTable.buttons[2][3] == tempTable.buttons[3][3] && tempTable.buttons[3][3] == tempTable.buttons[4][3] && tempTable.buttons[4][3] == tempTable.buttons[5][3] && tempTable.buttons[2][3] != "" && tempTable.buttons[3][3] != "" && tempTable.buttons[4][3] != "" && tempTable.buttons[5][3] != "")
            return true
        else if (tempTable.buttons[3][3] == tempTable.buttons[4][3] && tempTable.buttons[4][3] == tempTable.buttons[5][3] && tempTable.buttons[5][3] == tempTable.buttons[6][3] && tempTable.buttons[3][3] != "" && tempTable.buttons[4][3] != "" && tempTable.buttons[5][3] != "" && tempTable.buttons[6][3] != "")
            return true
        else if (tempTable.buttons[0][4] == tempTable.buttons[1][4] && tempTable.buttons[1][4] == tempTable.buttons[2][4] && tempTable.buttons[2][4] == tempTable.buttons[3][4] && tempTable.buttons[0][4] != "" && tempTable.buttons[1][4] != "" && tempTable.buttons[2][4] != "" && tempTable.buttons[3][4] != "")
            return true
        else if (tempTable.buttons[1][4] == tempTable.buttons[2][4] && tempTable.buttons[2][4] == tempTable.buttons[3][4] && tempTable.buttons[3][4] == tempTable.buttons[4][4] && tempTable.buttons[1][4] != "" && tempTable.buttons[2][4] != "" && tempTable.buttons[3][4] != "" && tempTable.buttons[4][4] != "")
            return true
        else if (tempTable.buttons[2][4] == tempTable.buttons[3][4] && tempTable.buttons[3][4] == tempTable.buttons[4][4] && tempTable.buttons[4][4] == tempTable.buttons[5][4] && tempTable.buttons[2][4] != "" && tempTable.buttons[3][4] != "" && tempTable.buttons[4][4] != "" && tempTable.buttons[5][4] != "")
            return true
        else if (tempTable.buttons[3][4] == tempTable.buttons[4][4] && tempTable.buttons[4][4] == tempTable.buttons[5][4] && tempTable.buttons[5][4] == tempTable.buttons[6][4] && tempTable.buttons[3][4] != "" && tempTable.buttons[4][4] != "" && tempTable.buttons[5][4] != "" && tempTable.buttons[6][4] != "")
            return true
        else if (tempTable.buttons[0][5] == tempTable.buttons[1][5] && tempTable.buttons[1][5] == tempTable.buttons[2][5] && tempTable.buttons[2][5] == tempTable.buttons[3][5] && tempTable.buttons[0][5] != "" && tempTable.buttons[1][5] != "" && tempTable.buttons[2][5] != "" && tempTable.buttons[3][5] != "")
            return true
        else if (tempTable.buttons[1][5] == tempTable.buttons[2][5] && tempTable.buttons[2][5] == tempTable.buttons[3][5] && tempTable.buttons[3][5] == tempTable.buttons[4][5] && tempTable.buttons[1][5] != "" && tempTable.buttons[2][5] != "" && tempTable.buttons[3][5] != "" && tempTable.buttons[4][5] != "")
            return true
        else if (tempTable.buttons[2][5] == tempTable.buttons[3][5] && tempTable.buttons[3][5] == tempTable.buttons[4][5] && tempTable.buttons[4][5] == tempTable.buttons[5][5] && tempTable.buttons[2][5] != "" && tempTable.buttons[3][5] != "" && tempTable.buttons[4][5] != "" && tempTable.buttons[5][5] != "")
            return true
        else if (tempTable.buttons[3][5] == tempTable.buttons[4][5] && tempTable.buttons[4][5] == tempTable.buttons[5][5] && tempTable.buttons[5][5] == tempTable.buttons[6][5] && tempTable.buttons[3][5] != "" && tempTable.buttons[4][5] != "" && tempTable.buttons[5][5] != "" && tempTable.buttons[6][5] != "")
            return true
        else if (tempTable.buttons[0][6] == tempTable.buttons[1][6] && tempTable.buttons[1][6] == tempTable.buttons[2][6] && tempTable.buttons[2][6] == tempTable.buttons[3][6] && tempTable.buttons[0][6] != "" && tempTable.buttons[1][6] != "" && tempTable.buttons[2][6] != "" && tempTable.buttons[3][6] != "")
            return true
        else if (tempTable.buttons[1][6] == tempTable.buttons[2][6] && tempTable.buttons[2][6] == tempTable.buttons[3][6] && tempTable.buttons[3][6] == tempTable.buttons[4][6] && tempTable.buttons[1][6] != "" && tempTable.buttons[2][6] != "" && tempTable.buttons[3][6] != "" && tempTable.buttons[4][6] != "")
            return true
        else if (tempTable.buttons[2][6] == tempTable.buttons[3][6] && tempTable.buttons[3][6] == tempTable.buttons[4][6] && tempTable.buttons[4][6] == tempTable.buttons[5][6] && tempTable.buttons[2][6] != "" && tempTable.buttons[3][6] != "" && tempTable.buttons[4][6] != "" && tempTable.buttons[5][6] != "")
            return true
        else if (tempTable.buttons[3][6] == tempTable.buttons[4][6] && tempTable.buttons[4][6] == tempTable.buttons[5][6] && tempTable.buttons[5][6] == tempTable.buttons[6][6] && tempTable.buttons[3][6] != "" && tempTable.buttons[4][6] != "" && tempTable.buttons[5][6] != "" && tempTable.buttons[6][6] != "")
            return true
        else if (tempTable.buttons[3][0] == tempTable.buttons[2][1] && tempTable.buttons[2][1] == tempTable.buttons[1][2] && tempTable.buttons[1][2] == tempTable.buttons[0][3] && tempTable.buttons[3][0] != "" && tempTable.buttons[2][1] != "" && tempTable.buttons[1][2] != "" && tempTable.buttons[0][3] != "")
            return true
        else if (tempTable.buttons[4][0] == tempTable.buttons[3][1] && tempTable.buttons[3][1] == tempTable.buttons[2][2] && tempTable.buttons[2][2] == tempTable.buttons[1][3] && tempTable.buttons[4][0] != "" && tempTable.buttons[3][1] != "" && tempTable.buttons[2][2] != "" && tempTable.buttons[1][3] != "")
            return true
        else if (tempTable.buttons[3][1] == tempTable.buttons[2][2] && tempTable.buttons[2][2] == tempTable.buttons[1][3] && tempTable.buttons[1][3] == tempTable.buttons[0][4] && tempTable.buttons[3][1] != "" && tempTable.buttons[2][2] != "" && tempTable.buttons[1][3] != "" && tempTable.buttons[0][4] != "")
            return true
        else if (tempTable.buttons[5][0] == tempTable.buttons[4][1] && tempTable.buttons[4][1] == tempTable.buttons[3][2] && tempTable.buttons[3][2] == tempTable.buttons[2][3] && tempTable.buttons[5][0] != "" && tempTable.buttons[4][1] != "" && tempTable.buttons[3][2] != "" && tempTable.buttons[2][3] != "")
            return true
        else if (tempTable.buttons[4][1] == tempTable.buttons[3][2] && tempTable.buttons[3][2] == tempTable.buttons[2][3] && tempTable.buttons[2][3] == tempTable.buttons[1][4] && tempTable.buttons[4][1] != "" && tempTable.buttons[3][2] != "" && tempTable.buttons[2][3] != "" && tempTable.buttons[1][4] != "")
            return true
        else if (tempTable.buttons[3][2] == tempTable.buttons[2][3] && tempTable.buttons[2][3] == tempTable.buttons[1][4] && tempTable.buttons[1][4] == tempTable.buttons[0][5] && tempTable.buttons[3][2] != "" && tempTable.buttons[2][3] != "" && tempTable.buttons[1][4] != "" && tempTable.buttons[0][5] != "")
            return true
        else if (tempTable.buttons[6][0] == tempTable.buttons[5][1] && tempTable.buttons[5][1] == tempTable.buttons[4][2] && tempTable.buttons[4][2] == tempTable.buttons[3][3] && tempTable.buttons[6][0] != "" && tempTable.buttons[5][1] != "" && tempTable.buttons[4][2] != "" && tempTable.buttons[3][3] != "")
            return true
        else if (tempTable.buttons[5][1] == tempTable.buttons[4][2] && tempTable.buttons[4][2] == tempTable.buttons[3][3] && tempTable.buttons[3][3] == tempTable.buttons[2][4] && tempTable.buttons[5][1] != "" && tempTable.buttons[4][2] != "" && tempTable.buttons[3][3] != "" && tempTable.buttons[2][4] != "")
            return true
        else if (tempTable.buttons[4][2] == tempTable.buttons[3][3] && tempTable.buttons[3][3] == tempTable.buttons[2][4] && tempTable.buttons[2][4] == tempTable.buttons[1][5] && tempTable.buttons[4][2] != "" && tempTable.buttons[3][3] != "" && tempTable.buttons[2][4] != "" && tempTable.buttons[1][5] != "")
            return true
        else if (tempTable.buttons[3][3] == tempTable.buttons[2][4] && tempTable.buttons[2][4] == tempTable.buttons[1][5] && tempTable.buttons[1][5] == tempTable.buttons[0][6] && tempTable.buttons[3][3] != "" && tempTable.buttons[2][4] != "" && tempTable.buttons[1][5] != "" && tempTable.buttons[0][6] != "")
            return true
        else if (tempTable.buttons[6][1] == tempTable.buttons[5][2] && tempTable.buttons[5][2] == tempTable.buttons[4][3] && tempTable.buttons[4][3] == tempTable.buttons[3][4] && tempTable.buttons[6][1] != "" && tempTable.buttons[5][2] != "" && tempTable.buttons[4][3] != "" && tempTable.buttons[3][4] != "")
            return true
        else if (tempTable.buttons[5][2] == tempTable.buttons[4][3] && tempTable.buttons[4][3] == tempTable.buttons[3][4] && tempTable.buttons[3][4] == tempTable.buttons[2][5] && tempTable.buttons[5][2] != "" && tempTable.buttons[4][3] != "" && tempTable.buttons[3][4] != "" && tempTable.buttons[2][5] != "")
            return true
        else if (tempTable.buttons[4][3] == tempTable.buttons[3][4] && tempTable.buttons[3][4] == tempTable.buttons[2][5] && tempTable.buttons[2][5] == tempTable.buttons[1][6] && tempTable.buttons[4][3] != "" && tempTable.buttons[3][4] != "" && tempTable.buttons[2][5] != "" && tempTable.buttons[1][6] != "")
            return true
        else if (tempTable.buttons[6][2] == tempTable.buttons[5][3] && tempTable.buttons[5][3] == tempTable.buttons[4][4] && tempTable.buttons[4][4] == tempTable.buttons[3][5] && tempTable.buttons[6][2] != "" && tempTable.buttons[5][3] != "" && tempTable.buttons[4][4] != "" && tempTable.buttons[3][5] != "")
            return true
        else if (tempTable.buttons[5][3] == tempTable.buttons[4][4] && tempTable.buttons[4][4] == tempTable.buttons[3][5] && tempTable.buttons[3][5] == tempTable.buttons[2][6] && tempTable.buttons[5][3] != "" && tempTable.buttons[4][4] != "" && tempTable.buttons[3][5] != "" && tempTable.buttons[2][6] != "")
            return true
        else if (tempTable.buttons[6][3] == tempTable.buttons[5][4] && tempTable.buttons[5][4] == tempTable.buttons[4][5] && tempTable.buttons[4][5] == tempTable.buttons[3][6] && tempTable.buttons[6][3] != "" && tempTable.buttons[5][4] != "" && tempTable.buttons[4][5] != "" && tempTable.buttons[3][6] != "")
            return true
        else if (tempTable.buttons[0][3] == tempTable.buttons[1][4] && tempTable.buttons[1][4] == tempTable.buttons[2][5] && tempTable.buttons[2][5] == tempTable.buttons[3][6] && tempTable.buttons[0][3] != "" && tempTable.buttons[1][4] != "" && tempTable.buttons[2][5] != "" && tempTable.buttons[3][6] != "")
            return true
        else if (tempTable.buttons[0][2] == tempTable.buttons[1][3] && tempTable.buttons[1][3] == tempTable.buttons[2][4] && tempTable.buttons[2][4] == tempTable.buttons[3][5] && tempTable.buttons[0][2] != "" && tempTable.buttons[1][3] != "" && tempTable.buttons[2][4] != "" && tempTable.buttons[3][5] != "")
            return true
        else if (tempTable.buttons[1][3] == tempTable.buttons[2][4] && tempTable.buttons[2][4] == tempTable.buttons[3][5] && tempTable.buttons[3][5] == tempTable.buttons[4][6] && tempTable.buttons[1][3] != "" && tempTable.buttons[2][4] != "" && tempTable.buttons[3][5] != "" && tempTable.buttons[4][6] != "")
            return true
        else if (tempTable.buttons[0][1] == tempTable.buttons[1][2] && tempTable.buttons[1][2] == tempTable.buttons[2][3] && tempTable.buttons[2][3] == tempTable.buttons[3][4] && tempTable.buttons[0][1] != "" && tempTable.buttons[1][2] != "" && tempTable.buttons[2][3] != "" && tempTable.buttons[3][4] != "")
            return true
        else if (tempTable.buttons[1][2] == tempTable.buttons[2][3] && tempTable.buttons[2][3] == tempTable.buttons[3][4] && tempTable.buttons[3][4] == tempTable.buttons[4][5] && tempTable.buttons[1][2] != "" && tempTable.buttons[2][3] != "" && tempTable.buttons[3][4] != "" && tempTable.buttons[4][5] != "")
            return true
        else if (tempTable.buttons[2][3] == tempTable.buttons[3][4] && tempTable.buttons[3][4] == tempTable.buttons[4][5] && tempTable.buttons[4][5] == tempTable.buttons[5][6] && tempTable.buttons[2][3] != "" && tempTable.buttons[3][4] != "" && tempTable.buttons[4][5] != "" && tempTable.buttons[5][6] != "")
            return true
        else if (tempTable.buttons[0][0] == tempTable.buttons[1][1] && tempTable.buttons[1][1] == tempTable.buttons[2][2] && tempTable.buttons[2][2] == tempTable.buttons[3][3] && tempTable.buttons[0][0] != "" && tempTable.buttons[1][1] != "" && tempTable.buttons[2][2] != "" && tempTable.buttons[3][3] != "")
            return true
        else if (tempTable.buttons[1][1] == tempTable.buttons[2][2] && tempTable.buttons[2][2] == tempTable.buttons[3][3] && tempTable.buttons[3][3] == tempTable.buttons[4][4] && tempTable.buttons[1][1] != "" && tempTable.buttons[2][2] != "" && tempTable.buttons[3][3] != "" && tempTable.buttons[4][4] != "")
            return true
        else if (tempTable.buttons[2][2] == tempTable.buttons[3][3] && tempTable.buttons[3][3] == tempTable.buttons[4][4] && tempTable.buttons[4][4] == tempTable.buttons[5][5] && tempTable.buttons[2][2] != "" && tempTable.buttons[3][3] != "" && tempTable.buttons[4][4] != "" && tempTable.buttons[5][5] != "")
            return true
        else if (tempTable.buttons[3][3] == tempTable.buttons[4][4] && tempTable.buttons[4][4] == tempTable.buttons[5][5] && tempTable.buttons[5][5] == tempTable.buttons[6][6] && tempTable.buttons[3][3] != "" && tempTable.buttons[4][4] != "" && tempTable.buttons[5][5] != "" && tempTable.buttons[6][6] != "")
            return true
        else if (tempTable.buttons[1][0] == tempTable.buttons[2][1] && tempTable.buttons[2][1] == tempTable.buttons[3][2] && tempTable.buttons[3][2] == tempTable.buttons[4][3] && tempTable.buttons[1][0] != "" && tempTable.buttons[2][1] != "" && tempTable.buttons[3][2] != "" && tempTable.buttons[4][3] != "")
            return true
        else if (tempTable.buttons[2][1] == tempTable.buttons[3][2] && tempTable.buttons[3][2] == tempTable.buttons[4][3] && tempTable.buttons[4][3] == tempTable.buttons[4][4] && tempTable.buttons[2][1] != "" && tempTable.buttons[3][2] != "" && tempTable.buttons[4][3] != "" && tempTable.buttons[4][4] != "")
            return true
        else if (tempTable.buttons[3][2] == tempTable.buttons[4][3] && tempTable.buttons[4][3] == tempTable.buttons[5][4] && tempTable.buttons[5][4] == tempTable.buttons[6][5] && tempTable.buttons[3][2] != "" && tempTable.buttons[4][3] != "" && tempTable.buttons[5][4] != "" && tempTable.buttons[6][5] != "")
            return true
        else if (tempTable.buttons[2][0] == tempTable.buttons[3][1] && tempTable.buttons[3][1] == tempTable.buttons[4][2] && tempTable.buttons[4][2] == tempTable.buttons[5][3] && tempTable.buttons[2][0] != "" && tempTable.buttons[3][1] != "" && tempTable.buttons[4][2] != "" && tempTable.buttons[5][3] != "")
            return true
        else if (tempTable.buttons[3][1] == tempTable.buttons[4][2] && tempTable.buttons[4][2] == tempTable.buttons[5][3] && tempTable.buttons[5][3] == tempTable.buttons[6][4] && tempTable.buttons[3][1] != "" && tempTable.buttons[4][2] != "" && tempTable.buttons[5][3] != "" && tempTable.buttons[6][4] != "")
            return true
        else return tempTable.buttons[3][0] == tempTable.buttons[4][1] && tempTable.buttons[4][1] == tempTable.buttons[5][2] && tempTable.buttons[5][2] == tempTable.buttons[6][3] && tempTable.buttons[3][0] != "" && tempTable.buttons[4][1] != "" && tempTable.buttons[5][2] != "" && tempTable.buttons[6][3] != ""
    }
}