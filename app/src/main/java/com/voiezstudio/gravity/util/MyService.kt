package com.voiezstudio.gravity.util

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

class MyService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {

        val sharedPref = getSharedPreferences("gravity", Context.MODE_PRIVATE)
        val id = sharedPref.getString("id", "")
        val user = sharedPref.getString("user", "")
        if (id != "" && user != "") {
            val ref = FirebaseDatabase.getInstance().getReference("two_player/$id/$user" + "active")
            ref.setValue(false)
        }
        this.stopSelf()
        super.onTaskRemoved(rootIntent)
    }
}