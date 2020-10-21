package com.devhack.mlkitandroid.smartreply

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.devhack.mlkitandroid.R

class SmartReplyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_smart_reply)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, ChatFragment.newInstance())
                .commitNow()
        }
    }
}