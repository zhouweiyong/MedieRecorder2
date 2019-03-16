package com.nze.medierecorder2

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.nze.medierecorder2.recorder.MediaRecordActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn1_main.setOnClickListener {
            startActivity(Intent(this@MainActivity, MediaRecordActivity::class.java))
        }
    }
}
