package com.weiner.dlawallpaper

import android.graphics.*
import android.os.Handler
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import kotlin.math.*


class LiveWallpaperService : WallpaperService() {
    companion object {
        const val DEBUG = false
        const val ARRAY_SIZE = 300
        const val SCALE = 4f
        const val MAX_ITERATIONS = 500
        const val TAG = "com.weiner.dlawallpaper"
    }

    override fun onCreateEngine(): Engine {
        return MyWallpaperEngine()
    }


    inner class MyWallpaperEngine : Engine() {
        private var dots = Array(ARRAY_SIZE) {
            IntArray(
                ARRAY_SIZE
            )
        }

        private var bmp: Bitmap? = null
        private var canvas: Canvas? = null
        private val handler = Handler()
        private val drawRunner = Runnable { draw() }
        private var visible = true

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
            // current coordinates
            var x = 0
            var y = 0
            // center coordinates
            var xc = 0
            var yc = 0
            // neighbour coordinates
            var xn = 0
            var yn = 0
            // max radius of canvas
            var rmax = 0
            // max radius of cells
            var rm = 1.0
            var currentDotIndex = 0

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
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                xc = (ARRAY_SIZE - 1) / 2
                yc = (ARRAY_SIZE - 1) / 2
                rmax = Math.min(xc, yc) - 1
                if (dots[xc][yc] == 0) {
                    dots[xc][yc] = 1
                    if (DEBUG) Log.i(TAG, "Initial pixel at $xc,$yc")
                }
                if (DEBUG) Log.i(TAG, "rm = $rm, rmax = $rmax")
                if (rm < rmax) {
                    var flag = false
                    // pick up random coordinates in rm radius from center
                    val a = 3.14159265 * 2 * Math.random()
                    x = (xc + rm * cos(a)).toInt()
                    y = (yc + rm * sin(a)).toInt()
                    for (i in 0 until MAX_ITERATIONS) {
                        // randomly travel until max iterations count
                        val rand = (Math.random() * 8).toInt()
                        x += nx[rand]
                        y += ny[rand]
                        // if it is an ages of screen, stop
                        if (x < 0 || x > ARRAY_SIZE - 1 || y < 0 || y > ARRAY_SIZE - 1) {
                            break
                        }
                        // if it is empty cell
                        if (dots[x][y] == 0) {
                            //check the neighbors
                            for (k in 0..7) {
                                xn = x + nx[k]
                                yn = y + ny[k]
                                // and has not empty neighbors
                                if (xn >= 0 && yn >= 0 && xn < ARRAY_SIZE && yn < ARRAY_SIZE &&
                                    dots[xn][yn] != 0
                                ) {
                                    // now it is not empty cell
                                    dots[x][y] = currentDotIndex
                                    if (DEBUG) Log.i(TAG, "Draw dot $x $y $currentDotIndex")
                                    val i_screen =
                                        Math.round(c.width / 2 + (x - ARRAY_SIZE / 2) * SCALE)
                                            .toFloat()
                                    val j_screen =
                                        Math.round(c.height / 2 + (y - ARRAY_SIZE / 2) * SCALE)
                                            .toFloat()
                                    paint.color = getColorByNumber(currentDotIndex)
                                    canvas?.drawCircle(i_screen, j_screen, 0.75f * SCALE, paint)
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
            val freq = Math.PI * 2 / 1000 //TODO:  make it changeable from settings
            red = (sin(freq * number + 0) * 127 + 128).roundToInt()
            green = (sin(freq * number + 2) * 127 + 128).roundToInt()
            blue = (sin(freq * number + 4) * 127 + 128).roundToInt()
            return Color.rgb(red, green, blue)
        }
    }
}

