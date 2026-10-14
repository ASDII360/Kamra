package com.example.customgcam

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class MainActivity : AppCompatActivity() {

    private var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)
        }

        findViewById<Button>(R.id.btnCapture).setOnClickListener { takePhoto() }
        
        findViewById<TextView>(R.id.btnRaw).setOnClickListener {
            Toast.makeText(this, "RAW toggled", Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.btnRatio).setOnClickListener {
            Toast.makeText(this, "Ratio toggled", Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.btnFlash).setOnClickListener {
            Toast.makeText(this, "Flash toggled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(findViewById<PreviewView>(R.id.viewFinder).surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch (exc: Exception) {
                Toast.makeText(this, "Camera binding failed", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(externalMediaDirs.firstOrNull(), "${System.currentTimeMillis()}.png")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Toast.makeText(baseContext, "Capture failed", Toast.LENGTH_SHORT).show()
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                val processedBitmap = applyHdrEffect(bitmap, contrastFactor = 1.25f)

                photoFile.outputStream().use { out ->
                    processedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                Toast.makeText(baseContext, "Processed PNG Saved!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun applyHdrEffect(src: Bitmap, contrastFactor: Float): Bitmap {
        val width = src.width
        val height = src.height
        val bmOut = Bitmap.createBitmap(width, height, src.config)
        val canvas = Canvas(bmOut)
        val paint = Paint()

        val scale = contrastFactor
        val translate = (-0.5f * scale + 0.5f) * 255f
        
        val colorMatrix = ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))

        val saturation = ColorMatrix().apply { setSaturation(1.2f) }
        colorMatrix.postConcat(saturation)

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return bmOut
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        baseContext, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}
