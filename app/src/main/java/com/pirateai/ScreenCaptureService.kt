package com.pirateai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import android.content.pm.ServiceInfo
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class ScreenCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var worker: HandlerThread? = null
    private var handler: Handler? = null
    private var detector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
    )
    private val processing = AtomicBoolean(false)
    private var lastFrameAt = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopCapture()
            return START_NOT_STICKY
        }
        if (projection != null) return START_NOT_STICKY

        val code = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data: Intent? = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_DATA)
        }
        if (code == 0 || data == null) {
            VisionState.setStatus("Missing screen-capture approval")
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        val note = makeNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, note, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, note)
        }
        startProjection(code, data)
        return START_NOT_STICKY
    }

    private fun startProjection(code: Int, data: Intent) {
        try {
            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(code, data)
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
            val scale = minOf(1f, 1000f / metrics.widthPixels.toFloat())
            val width = (metrics.widthPixels * scale).toInt().coerceAtLeast(1)
            val height = (metrics.heightPixels * scale).toInt().coerceAtLeast(1)

            worker = HandlerThread("PirateVision").also { it.start() }
            handler = Handler(worker!!.looper)
            reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            reader!!.setOnImageAvailableListener({ source -> analyzeFrame(source) }, handler)
            projection!!.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() { stopCapture() }
            }, handler)
            display = projection!!.createVirtualDisplay(
                "PIRATE-AI-Screen",
                width, height, metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader!!.surface, null, handler
            )
            VisionState.setStatus("Capture active — waiting for first frame", running = true)
        } catch (error: Exception) {
            VisionState.setStatus("Capture failed: " + (error.message ?: "unknown"))
            stopCapture()
        }
    }

    private fun analyzeFrame(source: ImageReader) {
        // Drain every callback so skipped frames cannot fill ImageReader's small buffer.
        var image: Image? = null
        try {
            image = source.acquireLatestImage()
            if (image == null) return
            val now = System.currentTimeMillis()
            if (now - lastFrameAt < 500 || !processing.compareAndSet(false, true)) {
                image.close()
                image = null
                return
            }
            lastFrameAt = now
            val bitmap = imageToBitmap(image)
            val frameWidth = bitmap.width
            val frameHeight = bitmap.height
            image.close()
            image = null

            // Show the captured frame immediately. Previously the preview was only
            // published after ML Kit completed, making a slow/unavailable model look
            // exactly like a failed screen-capture pipeline.
            VisionState.publishFrame(bitmap, frameWidth, frameHeight)
            detector.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { objects ->
                    val boxes = objects.map { found ->
                        val label = found.labels.firstOrNull()
                        val rect = found.boundingBox
                        DetectionBox(label?.text ?: "Object", label?.confidence,
                            rect.left.toFloat(), rect.top.toFloat(), rect.right.toFloat(), rect.bottom.toFloat())
                    }
                    VisionState.publishDetections(boxes)
                }
                .addOnFailureListener { error ->
                    VisionState.setStatus("Frame captured; ML Kit error: " + (error.message ?: "unknown"), running = true)
                }
                .addOnCompleteListener { processing.set(false) }
        } catch (error: Exception) {
            image?.close()
            processing.set(false)
            VisionState.setStatus("Frame error: " + (error.message ?: "unknown"))
        }
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val plane = image.planes[0]
        val buffer: ByteBuffer = plane.buffer
        val paddedWidth = plane.rowStride / plane.pixelStride
        val padded = Bitmap.createBitmap(paddedWidth, image.height, Bitmap.Config.ARGB_8888)
        buffer.rewind()
        padded.copyPixelsFromBuffer(buffer)
        val result = Bitmap.createBitmap(padded, 0, 0, image.width, image.height)
        padded.recycle()
        return result
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "PIRATE-AI screen analysis", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun makeNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(this, CHANNEL_ID)
        else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder.setContentTitle("PIRATE-AI screen analysis")
            .setContentText("Screen capture is active. Tap STOP in the app to finish.")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
    }

    private fun stopCapture() {
        try { display?.release() } catch (_: Exception) {}
        display = null
        try { reader?.setOnImageAvailableListener(null, null); reader?.close() } catch (_: Exception) {}
        reader = null
        try { projection?.stop() } catch (_: Exception) {}
        projection = null
        try { detector.close() } catch (_: Exception) {}
        try { worker?.quitSafely() } catch (_: Exception) {}
        worker = null
        handler = null
        processing.set(false)
        VisionState.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (projection != null || display != null || reader != null) stopCapture()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.pirateai.START_CAPTURE"
        const val ACTION_STOP = "com.pirateai.STOP_CAPTURE"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "projection_data"
        private const val CHANNEL_ID = "pirate_capture"
        private const val NOTIFICATION_ID = 42
    }
}
