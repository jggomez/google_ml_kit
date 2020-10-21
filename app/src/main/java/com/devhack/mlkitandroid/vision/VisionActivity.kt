package com.devhack.mlkitandroid.vision

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.bumptech.glide.Glide
import com.devhack.mlkitandroid.R
import com.devhack.mlkitandroid.databinding.ActivityVisionBinding
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class VisionActivity : AppCompatActivity() {

    companion object {
        const val MY_PERMISSION = 999
        const val SELECT_PICTURE = 888
        const val REQUEST_TAKE_PHOTO = 777
        const val LANGUAGE_UNDEFINED = "und"
    }

    private var mCurrentPhotoPath: String? = null
    private var photoURI: Uri? = null
    private lateinit var binding: ActivityVisionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVisionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initListeners()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == MY_PERMISSION) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED
            ) {
                binding.btnDetectFaces.isEnabled = true
                binding.btnLabelImages.isEnabled = true
                binding.btnTranslate.isEnabled = true
                binding.btnRecognizeText.isEnabled = true
                binding.btnScanBarcode.isEnabled = true
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                SELECT_PICTURE -> {
                    photoURI = data?.data
                    Glide.with(this)
                        .load(photoURI)
                        .into(binding.btnImage)
                }
                REQUEST_TAKE_PHOTO -> Glide.with(this)
                    .load(photoURI)
                    .into(binding.btnImage)
            }
        }
    }

    private fun initListeners() {
        binding.btnRecognizeText.setOnClickListener { recognizeText() }
        //binding.btnScanBarcode.setOnClickListener { readCodeBars() }
        binding.btnDetectFaces.setOnClickListener { detectFaces() }
        binding.btnLabelImages.setOnClickListener { detectLabels() }
        binding.btnTranslate.setOnClickListener { translate() }

        binding.btnImage.setOnClickListener {
            if (requestPermission()) {
                binding.btnDetectFaces.isEnabled = true
                binding.btnLabelImages.isEnabled = true
                binding.btnTranslate.isEnabled = true
                binding.btnRecognizeText.isEnabled = true
                binding.btnScanBarcode.isEnabled = true

                MaterialDialog(this).show {
                    listItems(R.array.options_image) { _, index, _ ->
                        when (index) {
                            0 -> {
                                val intent = Intent(
                                    Intent.ACTION_PICK,
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                )
                                intent.type = "image/*"
                                startActivityForResult(
                                    Intent.createChooser(intent, "Selecciona una Imagen"),
                                    SELECT_PICTURE
                                )
                            }
                            else -> takePhoto()
                        }
                    }
                }
            }
        }
    }

    private fun detectLabels() {
        photoURI?.let {
            val image = InputImage.fromFilePath(this, it)

            // To use default options:
            val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

            labeler.process(image)
                .addOnSuccessListener { labels ->
                    Timber.i("*********** Labels *************")
                    val labelsDetect = mutableListOf<String>()
                    labels.forEach { label ->
                        val text = label.text
                        val confidence = label.confidence
                        val index = label.index

                        Timber.i("*********** Label *************")
                        Timber.i("Text => $text")
                        Timber.i("Confidence => $confidence")
                        Timber.i("index => $index")

                        labelsDetect.add("Label => $text | Confidence => $confidence")
                    }

                    MaterialDialog(this).show {
                        title(R.string.lbl_info)
                        message(text = labelsDetect.joinToString(" ****** "))
                        positiveButton(R.string.lbl_ok) {
                            dismiss()
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    MaterialDialog(this).show {
                        title(R.string.lbl_error)
                        message(text = exception.message)
                        positiveButton(R.string.lbl_ok) {
                            dismiss()
                        }
                    }
                }
        }
    }

    private fun detectFaces() {
        photoURI?.let {
            val image = InputImage.fromFilePath(this, it)

            // High-accuracy landmark detection and face classification
            val highAccuracyOpts = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()

            val detector = FaceDetection.getClient(highAccuracyOpts)

            detector.process(image)
                .addOnSuccessListener { faces ->
                    val numFaces = faces.size
                    Timber.i("*********** FACES => $numFaces *************")

                    faces.forEach { face ->
                        // If classification was enabled:
                        face.smilingProbability?.let { probability ->
                            Timber.i("*********** Smiling Probability => $probability *************")
                        }
                    }

                    MaterialDialog(this).show {
                        title(R.string.lbl_info)
                        message(text = "Faces => $numFaces")
                        positiveButton(R.string.lbl_ok) {
                            dismiss()
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    MaterialDialog(this).show {
                        title(R.string.lbl_error)
                        message(text = exception.message)
                        positiveButton(R.string.lbl_ok) {
                            dismiss()
                        }
                    }
                }

        }
    }

    private fun recognizeText() {

        photoURI?.let {
            val image = InputImage.fromFilePath(this, it)

            val recognizer = TextRecognition.getClient()
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val text = result.text
                    MaterialDialog(this).show {
                        title(R.string.lbl_info)
                        message(text = text)
                        positiveButton(R.string.lbl_ok) {
                            dismiss()
                        }
                    }

                    result.textBlocks.forEach { block ->
                        Timber.i("********* TextBlock *********")
                        Timber.i(block.text)
                        block.lines.forEach { line ->
                            Timber.i("********* Line *********")
                            Timber.i(line.text)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    MaterialDialog(this).show {
                        title(R.string.lbl_error)
                        message(text = exception.message)
                        positiveButton(R.string.lbl_ok) {
                            dismiss()
                        }
                    }
                }
        }
    }

    private fun requestPermission(): Boolean {

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }

        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
        ) {
            Toast.makeText(this, R.string.request_permission, Toast.LENGTH_SHORT).show()
            requestPermissions(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                ), MY_PERMISSION
            )
        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                ), MY_PERMISSION
            )
        }

        return false
    }

    private fun translate() {

        photoURI?.let {
            val image = InputImage.fromFilePath(this, it)

            val recognizer = TextRecognition.getClient()
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val text = result.text
                    val languageIdentifier = LanguageIdentification.getClient()
                    languageIdentifier.identifyLanguage(text)
                        .addOnSuccessListener { languageCode ->
                            if (languageCode == LANGUAGE_UNDEFINED) {
                                MaterialDialog(this).show {
                                    title(R.string.lbl_error)
                                    message(R.string.lbl_language_undefined)
                                    positiveButton(R.string.lbl_ok) {
                                        dismiss()
                                    }
                                }
                            } else {
                                val translationOptions =
                                    TranslatorOptions.Builder()
                                        .setSourceLanguage(TranslateLanguage.ENGLISH)
                                        .setTargetLanguage(TranslateLanguage.SPANISH)
                                        .build()

                                val spanishTranslator =
                                    Translation.getClient(translationOptions)

                                lifecycle.addObserver(spanishTranslator)

                                val conditions = DownloadConditions.Builder()
                                    .requireWifi()
                                    .build()

                                spanishTranslator
                                    .downloadModelIfNeeded(conditions)
                                    .addOnSuccessListener {
                                        spanishTranslator.translate(text)
                                            .addOnSuccessListener { translateText ->
                                                MaterialDialog(this).show {
                                                    title(text = "language code => $languageCode")
                                                    message(text = translateText)
                                                    positiveButton(R.string.lbl_ok) {
                                                        dismiss()
                                                    }
                                                }
                                            }
                                    }
                            }
                        }
                }
                .addOnFailureListener { exception ->
                    MaterialDialog(this).show {
                        title(R.string.lbl_error)
                        message(text = exception.message)
                        positiveButton(R.string.lbl_ok) {
                            dismiss()
                        }
                    }
                }
        }
    }

    private fun takePhoto() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.resolveActivity(packageManager).let {
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
                //Log.e()
            }

            photoFile.let {
                photoURI = FileProvider.getUriForFile(
                    this,
                    "com.devhack.mlkitandroid",
                    it!!
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
            }

        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "imgtask_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )

        mCurrentPhotoPath = image.absolutePath
        return image
    }
}