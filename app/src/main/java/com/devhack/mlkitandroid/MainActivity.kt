package com.devhack.mlkitandroid

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.devhack.mlkitandroid.digitalink.InkActivity
import com.devhack.mlkitandroid.smartreply.SmartReplyActivity
import com.devhack.mlkitandroid.vision.VisionActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        listener()
    }

    private fun listener() {
        btnDigitalInk.setOnClickListener {
            startActivity(Intent(this, InkActivity::class.java))
        }

        btnSmartReply.setOnClickListener {
            startActivity(Intent(this, SmartReplyActivity::class.java))
        }

        btnVision.setOnClickListener {
            startActivity(Intent(this, VisionActivity::class.java))
        }
    }
}