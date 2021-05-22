package brain_factory.face_recognition

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.File
import java.nio.FloatBuffer
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity()
{
    private val REQUEST_IMAGE_CAPTURE = 420

    private lateinit var addFaceButton: Button
    private lateinit var authenticateButton: Button

    private lateinit var currentPhotoPath: String

    private var faceBounds: Rect = Rect(0, 0, 1500, 1500)
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        this.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        addFaceButton = findViewById<View>(R.id.AddFaceButton) as Button
        authenticateButton = findViewById<View>(R.id.AuthenticateButton) as Button

        authenticateButton.setOnClickListener {
            dispatchTakePictureIntent()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK)
        {
            val bitmapImage = ImageUtils.handleSamplingAndRotationBitmap(this, currentPhotoPath)
            extractFace(bitmapImage)
            val faceNet = FaceNet(assets)
            val embeddings: FloatBuffer = faceNet.getEmbeddings(bitmapImage, faceBounds)
            Log.d("[embeddings]", embeddings[0].toString()) // DEBUG
            faceNet.close()

            val displayIntent = Intent(this, ImageTest::class.java).apply {
                putExtra("image_test", currentPhotoPath)
            }
            startActivity(displayIntent)
        }
    }

    private fun createImageFile(): File
    {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply { currentPhotoPath = absolutePath }
    }

    private fun dispatchTakePictureIntent()
    {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File = createImageFile()
                photoFile.also {
                    val photoURI = FileProvider.getUriForFile(this, "com.example.android.fileprovider", it)
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    private fun extractFace(imageBitmap: Bitmap)
    {
        val options = FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE).setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE).build()

        val image = InputImage.fromBitmap(imageBitmap, 0)
        val detector = FaceDetection.getClient(options)
        detector.process(image).addOnSuccessListener { faces ->
            for (face in faces)
            {
                faceBounds = face.boundingBox
            }
        }
    }
}