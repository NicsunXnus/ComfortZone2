package com.example.appv2.ui.startUpSplashScreen

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.example.appv2.MainActivity
import com.example.appv2.R

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)

        // This will get the LottieAnimationView from your layout
        val animationView = findViewById<LottieAnimationView>(R.id.lottieAnimationView)

        // Start the animation
        animationView.playAnimation()

        // You can set a delay to start MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 3140) // delaying for 3 seconds
    }
}