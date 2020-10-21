package com.devhack.mlkitandroid.digitalink

import com.google.android.gms.tasks.SuccessContinuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.Ink
import com.google.mlkit.vision.digitalink.RecognitionResult
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class RecognitionTask(private val recognizer: DigitalInkRecognizer?, private val ink: Ink) {

    private var currentResult: RecognizedInk? = null
    private val cancelled: AtomicBoolean = AtomicBoolean(false)
    private val done: AtomicBoolean = AtomicBoolean(false)

    fun cancel() {
        cancelled.set(true)
    }

    fun done(): Boolean {
        return done.get()
    }

    fun result(): RecognizedInk? {
        return currentResult
    }

    /** Helper class that stores an ink along with the corresponding recognized text.  */
    class RecognizedInk internal constructor(val ink: Ink, val text: String?)

    fun run(): Task<String?> {
        Timber.i("RecoTask.run")
        return recognizer!!
            .recognize(ink)
            .onSuccessTask(
                SuccessContinuation { result: RecognitionResult? ->
                    if (cancelled.get() || result == null || result.candidates.isEmpty()
                    ) {
                        return@SuccessContinuation Tasks.forResult<String?>(null)
                    }
                    currentResult =
                        RecognizedInk(
                            ink,
                            result.candidates[0]
                                .text
                        )
                    Timber.i("result: ${currentResult!!.text}")
                    done.set(
                        true
                    )
                    return@SuccessContinuation Tasks.forResult(currentResult!!.text)
                }
            )
    }
}
