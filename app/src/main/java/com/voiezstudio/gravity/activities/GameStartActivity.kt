package com.voiezstudio.gravity.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.database.*
import com.voiezstudio.gravity.R
import com.voiezstudio.gravity.model.Keys
import com.voiezstudio.gravity.model.Table
import com.voiezstudio.gravity.util.MyService
import com.voiezstudio.gravity.util.NetworkConnection
import java.util.*
import kotlin.collections.HashMap

class GameStartActivity : AppCompatActivity() {

    private lateinit var ltName: TextInputLayout
    private lateinit var ltRoomCode: TextInputLayout
    private lateinit var etName: EditText
    private lateinit var btnCreate: Button
    private lateinit var btnJoin: Button
    private lateinit var etRoomCode: EditText
    private lateinit var btnStart: Button
    private lateinit var progressJoin: ProgressBar
    private lateinit var btnInfo: Button

    var keys: Keys = Keys()
    var createdId: String? = null
    var host: String? = null
    var guest: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_start)

        val networkConnection = NetworkConnection(applicationContext)
        networkConnection.observe(this, { isConnected ->

            if (!isConnected) {

                val intent = Intent(this, NoNetworkActivity::class.java)
                intent.putExtra("activity", this.toString())
                startActivity(intent)
            }
        })

        startService(Intent(this, MyService::class.java))

        etName = findViewById(R.id.etName)
        btnCreate = findViewById(R.id.btnCreate)
        btnJoin = findViewById(R.id.btnJoin)
        etRoomCode = findViewById(R.id.etRoomCode)
        btnStart = findViewById(R.id.btnStart)
        ltName = findViewById(R.id.ltName)
        ltRoomCode = findViewById(R.id.ltRoomCode)
        progressJoin = findViewById(R.id.progressJoin)
        btnInfo = findViewById(R.id.btnInfo)

        ltRoomCode.visibility = View.GONE
        btnStart.visibility = View.GONE
        progressJoin.visibility = View.GONE

        val ref = FirebaseDatabase.getInstance().getReference("two_player")
        ref.addValueEventListener(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    keys.keys.clear()
                    for (child: DataSnapshot in snapshot.children) {

                        keys.keys.add(child.key.toString())

                        val newtable: Table = child.getValue(Table::class.java)!!
                        val time = System.currentTimeMillis()

                        if (!(newtable.hostactive || newtable.guestactive) ||
                            (!(newtable.hostactive && newtable.guestactive) && newtable.player1 != "" && newtable.player2 != "") ||
                            ((time - newtable.time) > 300000 && newtable.player2 == "")
                        ) {

                            val newRef = FirebaseDatabase.getInstance()
                                .getReference("two_player/" + child.key.toString())
                            newRef.removeValue()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        baseContext, "Error!$error",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
        btnCreate.setOnClickListener {

            ltRoomCode.visibility = View.GONE
            btnStart.visibility = View.GONE
            progressJoin.visibility = View.GONE

            if (nameCheck()) {

                progressJoin.visibility = View.VISIBLE

                host = etName.text.toString()
                val time = System.currentTimeMillis()
                val table = Table(time, "$host", true)
                createdId = ref.push().key

                if (createdId != null) {

                    ref.child(createdId.toString())
                        .setValue(table).addOnCompleteListener {

                            val sharedPref =
                                getSharedPreferences("gravity", Context.MODE_PRIVATE).edit()
                            sharedPref.putString("id", createdId.toString()).apply()
                            sharedPref.putString("user", "host").apply()

                            Toast.makeText(
                                baseContext, "Room Created!",
                                Toast.LENGTH_SHORT
                            ).show()
                            val intent = Intent(this, WaitActivity::class.java)
                            intent.putExtra("id", createdId.toString())
                            startActivity(intent)
                            finish()
                        }
                }
            }
        }
        btnJoin.setOnClickListener {

            progressJoin.visibility = View.GONE
            ltRoomCode.visibility = View.GONE

            val tempRef =
                FirebaseDatabase.getInstance().getReference("two_player/" + ref.push().key)
            tempRef.removeValue()
            if (nameCheck()) {

                ltRoomCode.visibility = View.VISIBLE
                btnStart.visibility = View.VISIBLE
            }
        }
        btnStart.setOnClickListener {

            if (roomCheck()) {

                btnStart.visibility = View.GONE
                progressJoin.visibility = View.VISIBLE

                val roomCode: String = etRoomCode.text.toString()

                val sharedPref = getSharedPreferences("gravity", Context.MODE_PRIVATE).edit()
                sharedPref.putString("id", roomCode).apply()
                sharedPref.putString("user", "guest").apply()

                guest = etName.text.toString()
                val hashMap: HashMap<String, Any> = HashMap()
                hashMap.put("player2", "$guest")
                hashMap.put("guestactive", true)

                ref.child(roomCode).updateChildren(hashMap).addOnSuccessListener {

                    btnStart.visibility = View.VISIBLE
                    progressJoin.visibility = View.GONE

                    Toast.makeText(
                        baseContext, "Joined Room!",
                        Toast.LENGTH_SHORT
                    ).show()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("user", "guest")
                    intent.putExtra("id", roomCode)
                    startActivity(intent)
                }
            }
        }
        btnInfo.setOnClickListener {

            startActivity(Intent(this, InfoActivity::class.java))
        }
    }

    private fun nameCheck(): Boolean {

        ltName.error = null
        if (etName.text.isEmpty()) {

            ltName.error = "Invalid Name!"
            return false
        } else
            return true
    }

    private fun roomCheck(): Boolean {

        ltRoomCode.error = null
        if (etRoomCode.text.isEmpty()) {

            ltRoomCode.error = "Invalid Room Code!"
            return false
        } else {
            for (ids: String in keys.keys) {
                if (ids == etRoomCode.text.toString()) {
                    return true
                }
            }
            ltRoomCode.error = "Invalid Room Code!"
            return false
        }

    }

    override fun onBackPressed() {
        finishAffinity()
        val sharedPref = getSharedPreferences("gravity", Context.MODE_PRIVATE)
        val id = sharedPref.getString("id", "")
        val user = sharedPref.getString("user", "")
        if (id != "" && user != "") {
            val ref = FirebaseDatabase.getInstance().getReference("two_player/$id/$user" + "active")
            ref.setValue(false)
        }
        super.onBackPressed()
    }
}