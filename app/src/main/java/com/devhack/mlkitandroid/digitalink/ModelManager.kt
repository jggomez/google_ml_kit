package com.devhack.mlkitandroid.digitalink

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.*
import timber.log.Timber
import java.util.*

class ModelManager {

    var recognizer: DigitalInkRecognizer? = null
    private var model: DigitalInkRecognitionModel? = null
    private val remoteModelManager = RemoteModelManager.getInstance()

    val downloadedModelLanguages: Task<Set<String>>
        get() = remoteModelManager
            .getDownloadedModels(DigitalInkRecognitionModel::class.java)
            .onSuccessTask { remoteModels: Set<DigitalInkRecognitionModel>? ->
                val result: MutableSet<String> = HashSet()
                for (model in remoteModels!!) {
                    result.add(model.modelIdentifier.languageTag)
                }
                Timber.i("Downloaded models for languages:$result")
                Tasks.forResult<Set<String>>(result.toSet())
            }

    fun setModel(languageTag: String): String {
        // Clear the old model and recognizer.
        model = null
        recognizer?.close()
        recognizer = null

        // Try to parse the languageTag and get a model from it.
        val modelIdentifier: DigitalInkRecognitionModelIdentifier?
        modelIdentifier = try {
            DigitalInkRecognitionModelIdentifier.fromLanguageTag(languageTag)
        } catch (e: MlKitException) {
            Timber.e("Failed to parse language '$languageTag'")
            return ""
        } ?: return "No model for language: $languageTag"

        // Initialize the model and recognizer.
        model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
        return model?.let {
            recognizer = DigitalInkRecognition.getClient(
                DigitalInkRecognizerOptions.builder(model!!).build()
            )
            Timber.i(
                "Model set for language '$languageTag' ('$modelIdentifier.languageTag')."
            )
            return "Model set for language: $languageTag"
        } ?: "Model is null"
    }

    fun checkIsModelDownloaded(): Task<Boolean?> {
        return remoteModelManager.isModelDownloaded(model!!)
    }

    fun deleteActiveModel(): Task<String?> {
        if (model == null) {
            Timber.i("Model not set")
            return Tasks.forResult("Model not set")
        }
        return checkIsModelDownloaded()
            .onSuccessTask { result: Boolean? ->
                if (!result!!) {
                    return@onSuccessTask Tasks.forResult("Model not downloaded yet")
                }
                remoteModelManager
                    .deleteDownloadedModel(model!!)
                    .onSuccessTask { _: Void? ->
                        Timber.i("Model successfully deleted")
                        Tasks.forResult(
                            "Model successfully deleted"
                        )
                    }
            }
            .addOnFailureListener { e: Exception ->
                Timber.e("Error while model deletion: $e")
            }
    }

    fun download(): Task<String?> =
        model?.let { model ->
            remoteModelManager
                .download(model, DownloadConditions.Builder().build())
                .onSuccessTask { _: Void? ->
                    Timber.i("Model download succeeded.")
                    Tasks.forResult("Downloaded model successfully")
                }
                .addOnFailureListener { e: Exception ->
                    Timber.e("Error while downloading the model: $e")
                }
        } ?: Tasks.forResult("Model not selected.")
}
