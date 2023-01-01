package com.weiner.dlawallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        println("start!")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val buttonInstall = findViewById<Button>(R.id.buttonInstall)
        buttonInstall.setOnClickListener {
            val intent = Intent(
                WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER
            )
            intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, LiveWallpaperService::class.java)
            )
            startActivity(intent)
        }
    }
}