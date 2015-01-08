package com.example.dmitryweiner.dlalivewallpaper;

import android.service.wallpaper.WallpaperService;
import android.os.Handler;
import android.graphics.*;
import android.view.SurfaceHolder;
import android.util.Log;

/**
 * Created by Dmitry Weiner on 08.01.2015.
 */
public class LiveWallpaperService extends WallpaperService
{
    static final int ARRAY_SIZE = 300;
    static final boolean DEBUG = false;
    private volatile int[][] dots = new int[ARRAY_SIZE][ARRAY_SIZE];
    private volatile Bitmap bmp = null;
    private static String TAG = "dla_tree";
    private float scale = 1.5f;
    int x, y;
    int xc, yc;
    int xn, yn;
    int maxIt = 500;//TODO:  make it mutable
    int rmax;
    double rm = 1;
    int currentDotIndex = 0;
    double a;

    public void onCreate()
    {
        super.onCreate();
    }

    public void onDestroy()
    {
        super.onDestroy();
    }

    public Engine onCreateEngine()
    {
        return new MyWallpaperEngine();
    }

    class MyWallpaperEngine extends Engine
    {

        private final Handler handler = new Handler();
        private final Runnable drawRunner = new Runnable() {
            @Override
            public void run() {
                draw();
            }
        };
        private boolean visible = true;

        MyWallpaperEngine()
        {
            x = 0; // initialize x position
            y = 0;  // initialize y position

        }


        public void onCreate(SurfaceHolder surfaceHolder)
        {
            super.onCreate(surfaceHolder);
        }

        @Override
        public void onVisibilityChanged(boolean visible)
        {
            this.visible = visible;
            // if screen wallpaper is visible then draw the image otherwise do not draw
            if (visible)
            {
                handler.post(drawRunner);
            }
            else
            {
                handler.removeCallbacks(drawRunner);
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder)
        {
            super.onSurfaceDestroyed(holder);
            this.visible = false;
            handler.removeCallbacks(drawRunner);
        }

        public void onOffsetsChanged(float xOffset, float yOffset, float xStep, float yStep, int xPixels, int yPixels)
        {
            draw();
        }

        void draw()
        {
            final SurfaceHolder holder = getSurfaceHolder();
            byte nx[] = {-1, -1, 0, 1, 1, 1, 0, -1};
            byte ny[] = {0, 1, 1, 1, 0, -1, -1, -1};

            if (DEBUG)
                Log.i(TAG, "begin draw()");
            Canvas c = null;
            try
            {
                c = holder.lockCanvas();
                if (bmp == null) {
                    bmp = Bitmap.createBitmap(c.getWidth(), c.getHeight(), Bitmap.Config.ARGB_8888);
                }
                // clear the canvas
                //c.drawColor(Color.BLACK);
                if (c != null)
                {
                    xc = (ARRAY_SIZE - 1) / 2;
                    yc = (ARRAY_SIZE - 1) / 2;
                    rmax = Math.min(xc, yc) - 1;
                    if (dots[xc][yc] == 0) {
                        dots[xc][yc] = 1;
                        if (DEBUG)
                            Log.i(TAG, "Initial pixel at " + xc + "," + yc);
                    }
                    if (DEBUG)
                        Log.i(TAG, "rm = " + rm + ", rmax" + rmax);
                    if (rm < rmax) {
                        boolean flag = false;
                        a = 3.14159265 * 2 * Math.random();
                        x = (int) (xc + rm * Math.cos(a));
                        y = (int) (yc + rm * Math.sin(a));
                        for (int i = 0; i < maxIt; i++){
                            byte rand = (byte) (Math.random() * 8);
                            x = x + nx[rand];
                            y = y + ny[rand];
                            if (x < 0 || x > (ARRAY_SIZE - 1) ||
                                    y < 0 || y > (ARRAY_SIZE - 1)) {
                                break;
                            }
                            if (dots[x][y] == 0) {
                                //check the neighbors
                                for (byte k = 0; k < 8; k++){
                                    xn = x + nx[k];
                                    yn = y + ny[k];
                                    if (    xn >= 0 && yn >= 0 &&
                                            xn < ARRAY_SIZE && yn < ARRAY_SIZE &&
                                            (dots[xn][yn] != 0)) {

                                        dots[x][y] = currentDotIndex;
                                        if (DEBUG)
                                            Log.i(TAG, "Draw dot "+ x + " " + y+ " " + currentDotIndex);
                                        Canvas localCanvas = new Canvas(bmp);
                                        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                                        float i_screen = Math.round(c.getWidth()/2 + (x-ARRAY_SIZE/2) * scale);
                                        float j_screen = Math.round(c.getHeight()/2 + (y-ARRAY_SIZE/2) * scale);
                                        paint.setColor(getColorByNumber(currentDotIndex));
                                        localCanvas.drawCircle(i_screen, j_screen, 0.8f*scale, paint);
                                        //c.drawBitmap(bmp, 0, 0, null);
                                        currentDotIndex++;
                                        double r;
                                        r = Math.sqrt((x - xc) * (x - xc) + (y - yc) * (y - yc));
                                        if (r > rm) {
                                            rm = r;
                                        }
                                        flag = true;
                                        break;
                                    }
                                }
                            }//if not on screen
                            if (flag) {
                                break;
                            }

                        }//for
                        c.drawBitmap(bmp, 0, 0, null);//NOTE: weird behaviour
                    } else {
                        dots = new int[ARRAY_SIZE][ARRAY_SIZE];
                        rm = 0;
                        x = y = 0;
                        bmp.eraseColor(Color.BLACK);
                    }
                }
            }
            finally
            {
                if (c != null) {
                    if (DEBUG)
                        Log.i(TAG, "post canvas");
                    holder.unlockCanvasAndPost(c);
                }
            }

            handler.removeCallbacks(drawRunner);
            if (visible)
            {
                handler.postDelayed(drawRunner, 50);
            }

        }

        public int getColorByNumber(int number) {
            int red, green, blue;
            double freq = 3.14159265*2/1000;//TODO:  make it changable from settings
            red   = (int) Math.round(Math.sin(freq*number + 0) * 127 + 128);
            green = (int) Math.round(Math.sin(freq*number + 2) * 127 + 128);
            blue  = (int) Math.round(Math.sin(freq*number + 4) * 127 + 128);
            return Color.rgb(red, green, blue);
        }
    }
}
