package com.devhack.mlkitandroid.digitalink

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class StatusTextView : AppCompatTextView, StrokeManager.StatusChangedListener {

    private var strokeManager: StrokeManager? = null

    constructor(context: Context) : super(context)
    constructor(context: Context?, attributeSet: AttributeSet?) : super(
        context!!,
        attributeSet
    )

    override fun onStatusChanged() {
        this.text = strokeManager!!.status
    }

    fun setStrokeManager(strokeManager: StrokeManager?) {
        this.strokeManager = strokeManager
    }
}