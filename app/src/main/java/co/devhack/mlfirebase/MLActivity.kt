package co.devhack.mlfirebase

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_ml.*
import kotlinx.android.synthetic.main.content_ml.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MLActivity : AppCompatActivity() {

    private val MY_PERMISSION = 999
    private val SELECT_PICTURE = 888
    private val REQUEST_TAKE_PHOTO = 777
    private var mCurrentPhotoPath: String? = null
    private var photoURI: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ml)
        setSupportActionBar(toolbar)

        btnRecognizeText.setOnClickListener { recognizeText() }
        btnScanBarcode.setOnClickListener { readCodeBars() }
        btnDetectFaces.setOnClickListener { detectFaces() }
        btnLabelImages.setOnClickListener { detectLabels() }

        btnImage.setOnClickListener {
            if (requestPermisssion()) {

                btnDetectFaces.isEnabled = true
                btnLabelImages.isEnabled = true
                btnLandMark.isEnabled = true
                btnRecognizeText.isEnabled = true
                btnScanBarcode.isEnabled = true

                MaterialDialog.Builder(this)
                        .title(R.string.select_img)
                        .items("Galery", "Take Photo")
                        .itemsCallbackSingleChoice(-1, { _, _, which, _ ->
                            if (which == 0) {
                                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                                intent.type = "image/*"
                                startActivityForResult(Intent.createChooser(intent, "Escoje la Imagen"),
                                        SELECT_PICTURE)
                            } else if (which == 1) {
                                takePhoto()
                            }
                            true
                        })
                        .show()


            }
        }
    }

    private fun recognizeText() {
        val image = photoURI?.let { FirebaseVisionImage.fromFilePath(this, it) }

        val detector = FirebaseVision.getInstance().onDeviceTextRecognizer

        image?.let {
            detector.processImage(it).addOnSuccessListener { firebaseVisionText ->

                val blocks = firebaseVisionText.textBlocks

                Log.i("ML_Text_FullText", firebaseVisionText.text)

                blocks.forEach {
                    Log.i("ML_RecognizeText Blocks", it.text)

                    it.lines.forEach {
                        Log.i("ML_RecognizeText Lines", it.text)

                        it.elements.forEach {
                            Log.i("ML_Text Elements", it.text)
                        }

                    }
                }


            }.addOnFailureListener { Log.e("MLActivity", it.message) }
        }

    }

    private fun detectFaces() {

        val image = photoURI?.let { FirebaseVisionImage.fromFilePath(this, it) }

        val options =
                FirebaseVisionFaceDetectorOptions.Builder()
                        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .build()

        val detector = FirebaseVision.getInstance().getVisionFaceDetector(options)

        image?.let {
            detector.detectInImage(it).addOnSuccessListener { faces ->

                Log.i("ML_RecognizeFaces Num", faces.size.toString())

            }.addOnFailureListener { Log.e("MLActivity", it.message) }
        }

    }

    private fun readCodeBars() {

        val options =
                FirebaseVisionBarcodeDetectorOptions.Builder()
                        .setBarcodeFormats(
                                FirebaseVisionBarcode.FORMAT_PDF417)
                        .build()

        try {

            val image = photoURI?.let { FirebaseVisionImage.fromFilePath(this, it) }

            val detector = FirebaseVision.getInstance()
                    .getVisionBarcodeDetector(options)

            image?.let {
                detector.detectInImage(it).addOnSuccessListener { barcodes ->
                    barcodes.forEach { Log.i("MLActivity_codebar", it.rawValue) }
                }.addOnFailureListener { Log.e("MLActivity", it.message) }
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun detectLabels() {

        try {

            val image = photoURI?.let { FirebaseVisionImage.fromFilePath(this, it) }

            val detector = FirebaseVision.getInstance().visionLabelDetector

            image?.let {
                detector.detectInImage(it).addOnSuccessListener { labels ->
                    labels.forEach {
                        Log.i("MLActivity_label", it.label)
                        Log.i("MLActivity_entityId", it.entityId)
                        Log.i("MLActivity_confidence", it.confidence.toString())
                    }
                }.addOnFailureListener { Log.e("MLActivity", it.message) }
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }


    }


    private fun takePhoto() {

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Ensure that there's a camera activity to handle the intent
        takePictureIntent.resolveActivity(packageManager).let {
            // Create the File where the photo should go
            var photoFile: File? = null

            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {

            }

            // Continue only if the File was successfully created
            photoFile.let {
                photoURI = FileProvider.getUriForFile(this,
                        "co.edu.ucc.todoapp.android.fileprovider",
                        it!!)
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
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                storageDir      /* directory */
        )

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.absolutePath
        return image
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_ml, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun requestPermisssion(): Boolean {

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            return true
        }

        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            Toast.makeText(this, R.string.request_permission, Toast.LENGTH_SHORT).show()
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA), MY_PERMISSION)
        } else {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA), MY_PERMISSION)
        }

        return false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == MY_PERMISSION) {
            if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                btnDetectFaces.isEnabled = true
                btnLabelImages.isEnabled = true
                btnLandMark.isEnabled = true
                btnRecognizeText.isEnabled = true
                btnScanBarcode.isEnabled = true
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                SELECT_PICTURE -> {
                    photoURI = data?.data
                    Picasso.get().load(photoURI).fit().into(btnImage)
                }
                REQUEST_TAKE_PHOTO -> Picasso.get().load(photoURI).fit().into(btnImage)
            }
        }
    }


}
