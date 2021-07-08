package com.voiezstudio.gravity.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.voiezstudio.gravity.R

class InfoActivity : AppCompatActivity() {

    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener {

            super.onBackPressed()
        }
    }

    override fun onBackPressed() {

    }
}