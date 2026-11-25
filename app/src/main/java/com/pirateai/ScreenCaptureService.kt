package com.pirateai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.util.concurrent.atomic.AtomicBoolean

class ScreenCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var display: VirtualDisplay? = null
    private val processing = AtomicBoolean(false)
    private var lastFrameAt = 0L
    private val detector by lazy {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .enableMultipleObjects()
            .build()
        ObjectDetection.getClient(options)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_START -> startCapture(intent)
        }
        return START_NOT_STICKY
    }

    private fun startCapture(intent: Intent) {
        if (projection != null) return
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification())

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        @Suppress("DEPRECATION")
        val data = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else {
            intent.getParcelableExtra(EXTRA_DATA)
        } ?: run { stopSelf(); return }

        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = manager.getMediaProjection(resultCode, data)

        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)

        reader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 3)
        reader?.setOnImageAvailableListener({ imageReader ->
            val image = imageReader.acquireLatestImage() ?: return@setOnImageAvailableListener
            val now = System.currentTimeMillis()
            if (now - lastFrameAt < FRAME_INTERVAL_MS || !processing.compareAndSet(false, true)) {
                image.close()
                return@setOnImageAvailableListener
            }
            lastFrameAt = now

            var bitmap: Bitmap? = null
            try {
                val plane = image.planes[0]
                val paddedWidth = plane.rowStride / plane.pixelStride
                val padded = Bitmap.createBitmap(paddedWidth, image.height, Bitmap.Config.ARGB_8888)
                padded.copyPixelsFromBuffer(plane.buffer)
                bitmap = Bitmap.createBitmap(padded, 0, 0, image.width, image.height)
                padded.recycle()
            } catch (error: Exception) {
                processing.set(false)
                sendAnalysis("Frame conversion error: ${error.localizedMessage ?: "unknown"}")
            } finally {
                image.close()
            }

            val frame = bitmap ?: return@setOnImageAvailableListener
            try {
                detector.process(InputImage.fromBitmap(frame, 0))
                    .addOnSuccessListener { objects ->
                        if (objects.isEmpty()) {
                            sendAnalysis("No objects detected in the current frame.")
                        } else {
                            val lines = objects.take(8).mapIndexed { index, item ->
                                val box = item.boundingBox
                                val label = item.labels.maxByOrNull { it.confidence }
                                val name = label?.text ?: "Object"
                                val confidence = label?.confidence?.let { "  ${(it * 100).toInt()}%" } ?: ""
                                val bounds = "x=${box.left}, y=${box.top}, w=${box.width()}, h=${box.height()}"
                                "${index + 1}. $name$confidence\n   $bounds"
                            }
                            sendAnalysis("Detected ${objects.size} object(s):\n" + lines.joinToString("\n"))
                        }
                    }
                    .addOnFailureListener { error ->
                        sendAnalysis("Detector error: ${error.localizedMessage ?: "unknown"}")
                    }
                    .addOnCompleteListener {
                        frame.recycle()
                        processing.set(false)
                    }
            } catch (error: Exception) {
                frame.recycle()
                processing.set(false)
                sendAnalysis("Analysis error: ${error.localizedMessage ?: "unknown"}")
            }
        }, null)

        projection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                stopSelf()
            }
        }, null)

        display = projection?.createVirtualDisplay(
            "PIRATE-AI-Capture",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader?.surface,
            null,
            null
        )
        sendAnalysis("Screen capture active. Waiting for first analysis frame…")
    }

    private fun sendAnalysis(message: String) {
        val intent = Intent(ACTION_ANALYSIS).setPackage(packageName)
            .putExtra(EXTRA_ANALYSIS, message)
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "PIRATE-AI screen capture", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun notification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder.setContentTitle("PIRATE-AI vision is active")
            .setContentText("On-device object detection is running. Stop it from the app.")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
    }

    override fun onDestroy() {
        display?.release()
        reader?.close()
        projection?.stop()
        detector.close()
        display = null
        reader = null
        projection = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.pirateai.START_CAPTURE"
        const val ACTION_STOP = "com.pirateai.STOP_CAPTURE"
        const val ACTION_ANALYSIS = "com.pirateai.ANALYSIS_UPDATE"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "projection_data"
        const val EXTRA_ANALYSIS = "analysis_text"
        private const val CHANNEL_ID = "pirate_capture"
        private const val NOTIFICATION_ID = 42
        private const val FRAME_INTERVAL_MS = 650L
    }
}
