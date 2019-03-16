package com.nze.medierecorder2.recorder

import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import com.nze.medierecorder2.R
import kotlinx.android.synthetic.main.activity_media_play.*
import java.io.File

/**
 * https://www.cnblogs.com/plokmju/p/android_SurfaceView.html
 *
 * MediaPlayer可以播放本地视频和网络视频
 */
class MediaPlayActivity : AppCompatActivity(), SurfaceHolder.Callback, SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    var fileUrl: String? = null
    val TAG = "zwy"
    val sv: SurfaceView by lazy { sv_play }
    val btn_play: Button by lazy { btn_play_play }
    val btn_pause: Button by lazy { btn_pause_play }
    val btn_replay: Button by lazy { btn_replay_play }
    val btn_stop: Button by lazy { btn_stop_play }
    val seekBar: SeekBar by lazy { seekBar_play }
    var currentPosition: Int = 0
    var isPlaying = false
    val surfaceHolder: SurfaceHolder by lazy { sv.holder }
    var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_play)
        intent?.let {
            fileUrl = it.getStringExtra("fileUrl")
        }

        btn_play.setOnClickListener(this)
        btn_pause.setOnClickListener(this)
        btn_replay.setOnClickListener(this)
        btn_stop.setOnClickListener(this)

        et_path.setText(fileUrl)
        surfaceHolder.addCallback(this)
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        seekBar.setOnSeekBarChangeListener(this)
    }


    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_play_play -> {
                play(0)
            }
            R.id.btn_pause_play -> {
                pause()
            }
            R.id.btn_replay_play -> {
                replay()
            }
            R.id.btn_stop_play -> {
                stop()
            }
        }
    }

    /**
     * 开始播放
     */
    fun play(msec: Int) {
        // 获取视频文件地址
//        val path = et_path.text.toString().trim()
        val file = File(fileUrl)
        if (!file.exists()) {
            Toast.makeText(this, "视频文件路径错误", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            mediaPlayer = MediaPlayer()
            mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
            // 设置播放的视频源
            mediaPlayer?.setDataSource(fileUrl)
            // 设置显示视频的SurfaceHolder
            mediaPlayer?.setDisplay(surfaceHolder)
            Log.i(TAG, "开始装载")
            //mediaPlayer.prepare();同步装载
            mediaPlayer?.prepareAsync()//异步装载
            mediaPlayer?.setOnPreparedListener {
                Log.i(TAG, "装载完成")
                mediaPlayer?.start()
                // 按照初始位置播放
                mediaPlayer?.seekTo(msec)
                // 设置进度条的最大进度为视频流的最大播放时长
                seekBar.max = mediaPlayer?.duration!!
                // 开始线程，更新进度条的刻度
                object : Thread() {
                    override fun run() {
                        try {
                            isPlaying = true
                            while (isPlaying) {
                                val current = mediaPlayer?.currentPosition
                                seekBar.progress = current!!

                                Thread.sleep(500)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                    }
                }.start()

                btn_play.isEnabled = false
            }
            mediaPlayer?.setOnCompletionListener {
                // 在播放完毕被回调
                btn_play.isEnabled = true
            }

            mediaPlayer?.setOnErrorListener { mp, what, extra ->
                // 发生错误重新播放
                play(0)
                isPlaying = false
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /*
     * 停止播放
     */
    protected fun stop() {
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            btn_play.isEnabled = true
            isPlaying = false
        }
    }

    /**
     * 重新开始播放
     */
    protected fun replay() {
        if (mediaPlayer != null && isPlaying) {
//            mediaPlayer?.stop()
//            mediaPlayer?.release()
//            mediaPlayer = null
            mediaPlayer?.seekTo(0)
            mediaPlayer?.start()
            Toast.makeText(this, "重新播放", Toast.LENGTH_SHORT).show()
            btn_pause.text = "暂停"

            return
        }
        isPlaying = false
        play(0)

    }

    /**
     * 暂停或继续
     */
    protected fun pause() {
        if (btn_pause.text.toString().trim().equals("继续")) {
            btn_pause.text = "暂停"
            mediaPlayer?.start()
            Toast.makeText(this, "继续播放", Toast.LENGTH_SHORT).show()
            return
        }
        if (mediaPlayer != null && mediaPlayer!!.isPlaying()) {
            mediaPlayer?.pause()
            btn_pause.text = "继续"
            Toast.makeText(this, "暂停播放", Toast.LENGTH_SHORT).show()
        }

    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        //// 销毁SurfaceHolder的时候记录当前的播放位置并停止播放
        if (mediaPlayer != null && mediaPlayer?.isPlaying()!!) {
            currentPosition = mediaPlayer?.getCurrentPosition()!!
            mediaPlayer?.stop()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        if (currentPosition > 0) {
            // 创建SurfaceHolder的时候，如果存在上次播放的位置，则按照上次播放位置进行播放
            play(currentPosition)
            currentPosition = 0
        }
    }

    //-----Seekbar--------
    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        // 当进度条停止修改的时候触发
        // 取得当前进度条的刻度
        val progress = seekBar!!.getProgress()
        if (mediaPlayer != null && isPlaying) {
            // 设置当前播放的位置
            mediaPlayer?.seekTo(progress)
        }
    }
}
