package com.nze.medierecorder2.recorder

import android.content.Intent
import android.hardware.Camera
import android.media.MediaRecorder
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import com.nze.medierecorder2.R
import kotlinx.android.synthetic.main.activity_media_play.*
import kotlinx.android.synthetic.main.activity_media_record.*
import java.io.File

/**
 * Android 自定义视频录制终极解决方案
 *https://blog.csdn.net/u014665060/article/details/53037864
 * mCamera.setDisplayOrientation(90); 这句必须要的。
 * 第二：
 *2.1竖屏情况下：
 *如果是前置摄像头：
 *mediaRecorder.setOrientationHint(270);
 *如果是后置摄像头：
 *mediaRecorder.setOrientationHint(90);
 *2.2横情况下：
 *如果是前置摄像头：
 *mediaRecorder.setOrientationHint(180);
 *如果是后置摄像头：
 *mediaRecorder.setOrientationHint(0);
 *
 */


/**
 * https://github.com/a741762308/MediaRecorder
 * https://github.com/Mr-WangZhe/Android_Movie
 * 拍照和录视频
 * https://github.com/CodingMankk/024-AndroidCameraPro
 */
class MediaRecordActivity : AppCompatActivity(), View.OnClickListener, SurfaceHolder.Callback {


    val surfaceView: SurfaceView by lazy { sv_amr }
    private var mSurfaceHolder: SurfaceHolder? = null
    private var mCamera: Camera? = null
    private var mRecorder: MediaRecorder? = null
    private val CAMERA_ID = 1
    val TAG = "zwy"
    private var mIsSufaceCreated = false
    private var mIsRecording = false//是否开始录制
    private val fileName: String
        get() {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString() + File.separator + "VideoRecorder")
            if (!dir.exists()) {
                //多级文件夹的创建
                dir.mkdirs()
            }
            var lastFileName = dir.path + File.separator + "VID_" + System.currentTimeMillis() + ".mp4"
            return lastFileName
        }
    private var lastFileName: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_record)
        btn_start_record.setOnClickListener(this)
        btn_stop_record.setOnClickListener(this)
        btn_play_record.setOnClickListener(this)


        mSurfaceHolder = surfaceView.getHolder()
        mSurfaceHolder?.addCallback(this)
        mSurfaceHolder?.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }


    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_start_record -> {
                if (mIsRecording) {
                    stopRecording()
                } else {
                    initMediaRecorder()
                    startRecording()
                }
            }
            R.id.btn_stop_record -> {
                if (mIsRecording) {
                    stopRecording()
                }
            }
            R.id.btn_play_record -> {
                val intent = Intent(this@MediaRecordActivity, MediaPlayActivity::class.java)
                intent.putExtra("fileUrl", lastFileName)
                startActivity(intent)
            }

        }
    }

    override fun onPause() {
        super.onPause()
        if (mIsRecording) {
            stopRecording()
        }
        stopPreview()
    }

    override fun onResume() {
        super.onResume()
        startPreview()
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        startPreview()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        mIsSufaceCreated = false
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        mIsSufaceCreated = true

    }

    //启动预览
    private fun startPreview() {
        //保证只有一个Camera对象
        if (mCamera != null || !mIsSufaceCreated) {
            Log.d(TAG, "startPreview will return")
            return
        }

        mCamera = Camera.open(CAMERA_ID)

        val parameters = mCamera?.getParameters()
        val size = getBestPreviewSize(1080, 1920, parameters!!)
        if (size != null) {
            parameters.setPreviewSize(size!!.width, size!!.height)
        }

        parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
        parameters.previewFrameRate = 20

        //设置相机预览方向
        mCamera?.setDisplayOrientation(90)

        mCamera?.setParameters(parameters)

        try {
            mCamera?.setPreviewDisplay(mSurfaceHolder)
            //          mCamera.setPreviewCallback(mPreviewCallback);
        } catch (e: Exception) {
            Log.d(TAG, e.message)
        }

        mCamera?.startPreview()
    }

    private fun getBestPreviewSize(width: Int, height: Int, parameters: Camera.Parameters): Camera.Size? {
        var result: Camera.Size? = null

        for (size in parameters.supportedPreviewSizes) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size
                } else {
                    val resultArea = result.width * result.height
                    val newArea = size.width * size.height

                    if (newArea > resultArea) {
                        result = size
                    }
                }
            }
        }

        return result
    }

    //停止预览
    private fun stopPreview() {
        //释放Camera对象
        if (mCamera != null) {
            try {
                mCamera!!.setPreviewDisplay(null)
            } catch (e: Exception) {
                Log.e(TAG, e.message)
            }

            mCamera!!.stopPreview()
            mCamera!!.release()
            mCamera = null
        }
    }


    //录制初始化
    fun initMediaRecorder() {
        mRecorder = MediaRecorder()//实例化
        mCamera?.unlock()
        //给Recorder设置Camera对象，保证录像跟预览的方向保持一致
        mRecorder?.setCamera(mCamera)
        mRecorder?.setOrientationHint(90)  //改变保存后的视频文件播放时是否横屏(不加这句，视频文件播放的时候角度是反的)
        mRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC) // 设置从麦克风采集声音
//        mRecorder?.setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
        mRecorder?.setVideoSource(MediaRecorder.VideoSource.CAMERA) // 设置从摄像头采集图像
        mRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)  // 设置视频的输出格式 为MP4
        mRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT) // 设置音频的编码格式
        //mRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264) // 设置视频的编码格式
        mRecorder?.setVideoEncodingBitRate(3 * 1024 * 1024)// 设置视频编码的比特率
        mRecorder?.setVideoSize(640, 480)  // 设置视频大小
        mRecorder?.setVideoFrameRate(20) // 设置帧率
        mRecorder?.setOrientationHint(270)// 设置选择角度，顺时针方向，默认是逆向90度。此处设置的是保存后的视频的角度
//        mRecorder.setMaxDuration(10000); //设置最大录像时间为10s
        mRecorder?.setPreviewDisplay(mSurfaceHolder?.getSurface())

        //设置视频存储路径
        lastFileName = fileName
        mRecorder?.setOutputFile(lastFileName)
    }

    //开始录制
    private fun startRecording() {
        if (mRecorder != null) {
            try {
                mRecorder?.prepare()
                mRecorder?.start()
            } catch (e: Exception) {
                mIsRecording = false
                Log.e(TAG, e.message)
            }
            tv_status.setText("正在录制")
        }

        mIsRecording = true
    }

    //结束录制
    fun stopRecording() {
        if (mCamera != null) {
            mCamera?.lock()
        }
        if (mRecorder != null) {
            mRecorder?.stop()
            mRecorder?.release()
            mRecorder = null
            tv_status.setText("录制完成")
        }

        mIsRecording = false
    }
}
