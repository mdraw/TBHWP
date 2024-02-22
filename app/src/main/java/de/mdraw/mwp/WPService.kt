package de.mdraw.mwp

import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import java.util.*


class WPService : WallpaperService() {
    override fun onCreateEngine(): Engine = WPEngine()

    inner class WPEngine : Engine() {

        private var paint = Paint()
        private var visible = true
        private var width = 0
        private var height = 0
        private val originalImage: Bitmap = BitmapFactory.decodeResource(
                resources,
                R.drawable.bg
        )
        private val blendMode = PorterDuff.Mode.MULTIPLY
        private val redrawDelay = 1000L * 60L // in milliseconds

        private val handler = Handler(Looper.getMainLooper())
        private val drawRunner = Runnable { draw() }

        init {
            handler.post(drawRunner)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            this.visible = visible
            if (visible) {
                handler.post(drawRunner)
            } else {
                handler.removeCallbacks(drawRunner)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            this.visible = false
            handler.removeCallbacks(drawRunner)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            this.width = width
            this.height = height
            super.onSurfaceChanged(holder, format, width, height)
        }

        private fun currentFilter(): PorterDuffColorFilter {
            val calendar: Calendar = Calendar.getInstance()
            val hours: Int = calendar.get(Calendar.HOUR_OF_DAY)
            val minutes: Int = calendar.get(Calendar.MINUTE)
            val timeInMinutes: Int = hours * 60 + minutes
            val maxTime: Float = 60f * 24f // number of minutes per day
            val maxHue = 360f
            val hue: Float = timeInMinutes.toFloat() / maxTime * maxHue

            val color: Int = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
            Log.i("color-filter", "timeInMinutes: $timeInMinutes -> hue: $hue")
            return PorterDuffColorFilter(color, blendMode)
        }

        private fun draw() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    val image = Bitmap.createScaledBitmap(originalImage, canvas.width, canvas.height, true)
                    paint.colorFilter = currentFilter()
                    canvas.drawBitmap(image, 0f, 0f, paint)
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
            handler.removeCallbacks(drawRunner)
            if (visible) handler.postDelayed(drawRunner, redrawDelay)
        }
    }

}
