package com.hereliesaz.noobwifinder.services

import android.app.Activity
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
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.hereliesaz.noobwifinder.R
import java.io.ByteArrayOutputStream

class ScreenCaptureService : Service() {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private lateinit var imageReader: ImageReader
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private var screenDensity: Int = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val insets = windowMetrics.windowInsets.getInsets(android.view.WindowInsets.Type.systemBars())
            screenWidth = windowMetrics.bounds.width()
            screenHeight = windowMetrics.bounds.height()
            screenDensity = resources.displayMetrics.densityDpi
        } else {
            val displayMetrics = resources.displayMetrics
            screenWidth = displayMetrics.widthPixels
            screenHeight = displayMetrics.heightPixels
            screenDensity = displayMetrics.densityDpi
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Capture")
            .setContentText("Capturing screen...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
        startForeground(NOTIFICATION_ID, notification)

        val resultCode = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data = intent?.getParcelableExtra<Intent>("data")

        if (resultCode == Activity.RESULT_OK && data != null) {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            startCapture()
        }

        return START_NOT_STICKY
    }

    private fun startCapture() {
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            screenWidth,
            screenHeight,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface,
            null,
            null
        )

        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * screenWidth
                val bitmap = Bitmap.createBitmap(
                    screenWidth + rowPadding / pixelStride,
                    screenHeight,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                image.close()

                // For now, just log that we have the bitmap
                Log.d("ScreenCaptureService", "Bitmap captured: ${bitmap.width}x${bitmap.height}")

                // In the next step we will broadcast this bitmap
                 val stream = ByteArrayOutputStream()
                 bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                 val byteArray = stream.toByteArray()
                 val broadcastIntent = Intent()
                 broadcastIntent.action = "com.hereliesaz.noobwifinder.SCREEN_CAPTURE"
                 broadcastIntent.putExtra("bitmap", byteArray)
                 sendBroadcast(broadcastIntent)


                stopSelf() // Stop the service after one capture
            }
        }, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        virtualDisplay?.release()
        mediaProjection?.stop()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Screen Capture Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "ScreenCaptureServiceChannel"
        private const val NOTIFICATION_ID = 1
    }
}
