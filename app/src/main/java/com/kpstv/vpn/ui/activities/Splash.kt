package com.kpstv.vpn.ui.activities

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.kpstv.vpn.R
import com.kpstv.vpn.extensions.utils.AppUtils.setEdgeToEdgeSystemUiFlags

class Splash : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setEdgeToEdgeSystemUiFlags()

    setContentView(R.layout.layout_splash)

    val imageView = findViewById<ImageView>(R.id.iv_splash)
    val bitmap = BitmapFactory.decodeResource(resources, R.drawable.logo)
    imageView.setImageBitmap(bitmap)
    window.setBackgroundDrawable(
      ColorDrawable(
        ContextCompat.getColor(this, R.color.white)
      )
    )
    imageView.doOnLayout {
//      avdSplash?.registerAnimationCallback(animationCallback)
//      avdSplash?.start()
    }
    Handler(Looper.getMainLooper()).postDelayed({
      // Navigate to the next activity or fragment
      startActivity(Intent(this@Splash, Main::class.java))
      finish() // Optional: Finish the splash screen activity
    }, 2000)
  }

  private val animationCallback = object : Animatable2Compat.AnimationCallback() {
    override fun onAnimationEnd(drawable: Drawable?) {
      super.onAnimationEnd(drawable)
      val intent = Intent(this@Splash, Main::class.java)
      startActivity(intent)
      finish()
    }
  }
}