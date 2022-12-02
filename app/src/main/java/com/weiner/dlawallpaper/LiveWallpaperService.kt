package com.weiner.dlawallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.service.wallpaper.WallpaperService
import android.service.wallpaper.WallpaperService.Engine
import android.util.Log
import android.view.SurfaceHolder
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt


class LiveWallpaperService: WallpaperService() {
    companion object {
        const val ARRAY_SIZE = 300
        const val DEBUG = false
        const val TAG = "com.weiner.dlawallpaper"
    }


    private var dots = Array(ARRAY_SIZE) {
        IntArray(
            ARRAY_SIZE
        )
    }

    private var bmp: Bitmap? = null

    private var canvas: Canvas? = null
    private val scale = 2f
    var x = 0
    var y = 0
    var xc = 0
    var yc = 0
    var xn = 0
    var yn = 0
    var maxIt = 500 //TODO:  make it mutable

    var rmax = 0
    var rm = 1.0
    var currentDotIndex = 0
    var a = 0.0

    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onCreateEngine(): Engine {
        return MyWallpaperEngine()
    }


    inner class MyWallpaperEngine : Engine() {
        private val handler = Handler()
        private val drawRunner = Runnable { draw() }
        private var visible = true

        init {
            x = 0 // initialize x position
            y = 0 // initialize y position
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            // if screen wallpaper is visible then draw the image otherwise do not draw
            if (visible) {
                handler.post(drawRunner)
            } else {
                handler.removeCallbacks(drawRunner)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(drawRunner)
        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xStep: Float,
            yStep: Float,
            xPixels: Int,
            yPixels: Int
        ) {
            draw()
        }

        fun draw() {
            val holder = surfaceHolder
            val nx = byteArrayOf(-1, -1, 0, 1, 1, 1, 0, -1)
            val ny = byteArrayOf(0, 1, 1, 1, 0, -1, -1, -1)
            if (DEBUG) Log.i(TAG, "begin draw()")
            var c: Canvas? = null
            try {
                c = holder.lockCanvas()
                if (bmp == null) {
                    bmp = Bitmap.createBitmap(c.getWidth(), c.getHeight(), Bitmap.Config.ARGB_8888)
                    bmp?.eraseColor(Color.BLACK)
                    canvas = Canvas(bmp!!)
                }
                // clear the canvas
                //c.drawColor(Color.BLACK);
                if (c != null) {
                    xc = (ARRAY_SIZE - 1) / 2
                    yc = (ARRAY_SIZE - 1) / 2
                    rmax = Math.min(xc, yc) - 1
                    if (dots[xc][yc] == 0) {
                        dots[xc][yc] = 1
                        if (DEBUG) Log.i(TAG, "Initial pixel at $xc,$yc")
                    }
                    if (DEBUG) Log.i(TAG, "rm = $rm, rmax$rmax")
                    if (rm < rmax) {
                        var flag = false
                        a = 3.14159265 * 2 * Math.random()
                        x = (xc + rm * Math.cos(a)).toInt()
                        y = (yc + rm * Math.sin(a)).toInt()
                        for (i in 0 until maxIt) {
                            val rand = (Math.random() * 8).toInt()
                            x += nx[rand]
                            y += ny[rand]
                            if (x < 0 || x > ARRAY_SIZE - 1 || y < 0 || y > ARRAY_SIZE - 1) {
                                break
                            }
                            if (dots[x][y] == 0) {
                                //check the neighbors
                                for (k in 0..7) {
                                    xn = x + nx[k]
                                    yn = y + ny[k]
                                    if (xn >= 0 && yn >= 0 && xn < ARRAY_SIZE && yn < ARRAY_SIZE &&
                                        dots[xn][yn] != 0
                                    ) {
                                        dots[x][y] = currentDotIndex
                                        if (DEBUG) Log.i(TAG, "Draw dot $x $y $currentDotIndex")
                                        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                                        val i_screen =
                                            Math.round(c.getWidth() / 2 + (x - ARRAY_SIZE / 2) * scale)
                                                .toFloat()
                                        val j_screen =
                                            Math.round(c.getHeight() / 2 + (y - ARRAY_SIZE / 2) * scale)
                                                .toFloat()
                                        paint.setColor(getColorByNumber(currentDotIndex))
                                        canvas?.drawCircle(i_screen, j_screen, 0.75f * scale, paint)
                                        currentDotIndex++
                                        val r: Double =
                                            sqrt(((x - xc) * (x - xc) + (y - yc) * (y - yc)).toDouble())
                                        if (r > rm) {
                                            rm = r
                                        }
                                        flag = true
                                        break
                                    }
                                }
                            } //if not on screen
                            if (flag) {
                                break
                            }
                        } //for
                        c.drawBitmap(bmp!!, 0.0f, 0.0f, null) //NOTE: weird behaviour
                    } else {
                        dots = Array(ARRAY_SIZE) { IntArray(ARRAY_SIZE) }
                        rm = 0.0
                        y = 0
                        x = y
                        currentDotIndex = 0
                        bmp?.eraseColor(Color.BLACK)
                    }
                }
            } finally {
                if (c != null) {
                    if (DEBUG) Log.i(TAG, "post canvas")
                    holder.unlockCanvasAndPost(c)
                }
            }
            handler.removeCallbacks(drawRunner)
            if (visible) {
                handler.postDelayed(drawRunner, 50)
            }
        }

        private fun getColorByNumber(number: Int): Int {
            val red: Int
            val green: Int
            val blue: Int
            val freq = 3.14159265 * 2 / 1000 //TODO:  make it changable from settings
            red = (sin(freq * number + 0) * 127 + 128).roundToInt()
            green = (sin(freq * number + 2) * 127 + 128).roundToInt()
            blue = (sin(freq * number + 4) * 127 + 128).roundToInt()
            return Color.rgb(red, green, blue)
        }
    }
}

