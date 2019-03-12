package com.nze.medierecorder2

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
import kotlinx.android.synthetic.main.activity_media_record.*
import java.io.File

class MediaRecordActivity : AppCompatActivity(), View.OnClickListener, SurfaceHolder.Callback {


    val surfaceView: SurfaceView by lazy { sv_amr }
    private var mSurfaceHolder: SurfaceHolder? = null
    private var mCamera: Camera? = null
    private var mRecorder: MediaRecorder? = null
    private val CAMERA_ID = 1
    val TAG = "zwy"
    private var mIsSufaceCreated = false
    private var mIsRecording = false//是否开始录制
    private var lastFileName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_record)
        btn_start.setOnClickListener(this)
        btn_pause.setOnClickListener(this)
        btn_stop.setOnClickListener(this)
        btn_resume.setOnClickListener(this)

        mSurfaceHolder = surfaceView.getHolder()
        mSurfaceHolder?.addCallback(this)
        mSurfaceHolder?.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }


    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_start -> {
                if (mIsRecording) {
                    stopRecording()
                } else {
                    initMediaRecorder()
                    startRecording()
                }
            }
            R.id.btn_stop -> {
                if (mIsRecording) {
                    stopRecording()
                }
            }
            R.id.btn_pause -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mRecorder?.pause()
                    tv_status.setText("暂停")
                }
            }
            R.id.btn_resume -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mRecorder?.resume()
                    tv_status.setText("正在录制")
                }
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

    fun initMediaRecorder() {
        mRecorder = MediaRecorder()//实例化
        mCamera?.unlock()
        //给Recorder设置Camera对象，保证录像跟预览的方向保持一致
        mRecorder?.setCamera(mCamera)
        mRecorder?.setOrientationHint(90)  //改变保存后的视频文件播放时是否横屏(不加这句，视频文件播放的时候角度是反的)
        mRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC) // 设置从麦克风采集声音
        mRecorder?.setVideoSource(MediaRecorder.VideoSource.CAMERA) // 设置从摄像头采集图像
        mRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)  // 设置视频的输出格式 为MP4
        mRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT) // 设置音频的编码格式
        mRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264) // 设置视频的编码格式
        mRecorder?.setVideoEncodingBitRate(3 * 1024 * 1024)// 设置视频编码的比特率
        mRecorder?.setVideoSize(1280, 720)  // 设置视频大小
        mRecorder?.setVideoFrameRate(20) // 设置帧率
//        mRecorder.setMaxDuration(10000); //设置最大录像时间为10s
        mRecorder?.setPreviewDisplay(mSurfaceHolder?.getSurface())

        //设置视频存储路径
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString() + File.separator + "VideoRecorder")
        if (!file.exists()) {
            //多级文件夹的创建
            file.mkdirs()
        }
        lastFileName = file.path + File.separator + "VID_" + System.currentTimeMillis() + ".mp4"
        mRecorder?.setOutputFile(lastFileName)
    }

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

    fun stopRecording() {
        if (mCamera != null) {
            mCamera?.lock()
        }
        if (mRecorder != null) {
            mRecorder?.stop()
            mRecorder?.release()
            mRecorder = null
        }

        mIsRecording = false
    }
}
