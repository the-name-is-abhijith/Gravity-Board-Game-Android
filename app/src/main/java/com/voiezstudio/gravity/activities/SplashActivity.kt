package com.voiezstudio.gravity.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.VideoView
import androidx.appcompat.app.AppCompatDelegate
import com.voiezstudio.gravity.R

class SplashActivity : AppCompatActivity() {

    private lateinit var vvGravity : VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        vvGravity = findViewById(R.id.vvGravity)

        val video = Uri.parse("android.resource://" + packageName + "/" + R.raw.gravity)
        vvGravity.setVideoURI(video)

        vvGravity.setOnCompletionListener {

            startActivity(Intent(this,
                GameStartActivity::class.java))
            finish()
        }
        vvGravity.start()
    }
}