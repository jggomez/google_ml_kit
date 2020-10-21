package com.devhack.mlkitandroid.smartreply

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.devhack.mlkitandroid.R

class Message(val text: String, val isLocalUser: Boolean, val timestamp: Long) {

    fun getIcon(context: Context): Drawable {
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_tag_faces_black_24dp)
            ?: throw IllegalStateException("Could not get drawable ic_tag_faces_black_24dp")

        when (isLocalUser) {
            true -> DrawableCompat.setTint(drawable, Color.BLUE)
            false -> DrawableCompat.setTint(drawable, Color.RED)
        }

        return drawable
    }
}
