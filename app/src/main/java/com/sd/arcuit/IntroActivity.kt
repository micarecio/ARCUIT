package com.sd.arcuit

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class IntroActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)

        startLoading()
    }

    private fun startLoading() {
        val totalTime = 3000L
        val interval = 50L

        object : CountDownTimer(totalTime, interval) {
            override fun onTick(millisUntilFinished: Long) {
                val progress = (((totalTime - millisUntilFinished).toFloat() / totalTime) * 100).toInt()
                progressBar.progress = progress
                progressText.text = "$progress%"
            }

            override fun onFinish() {
                progressBar.progress = 100
                progressText.text = "100%"

                startActivity(Intent(this@IntroActivity, MainActivity::class.java))
                finish()
            }
        }.start()
    }
}