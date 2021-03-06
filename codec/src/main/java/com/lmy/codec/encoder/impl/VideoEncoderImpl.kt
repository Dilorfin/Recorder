/*
 * Copyright (c) 2018-present, lmyooyo@gmail.com.
 *
 * This source code is licensed under the GPL license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.lmy.codec.encoder.impl

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaFormat
import android.opengl.EGLContext
import android.opengl.GLES20
import com.lmy.codec.encoder.Encoder
import com.lmy.codec.entity.CodecContext
import com.lmy.codec.entity.PresentationTimer
import com.lmy.codec.helper.CodecHelper
import com.lmy.codec.pipeline.Pipeline
import com.lmy.codec.pipeline.impl.EventPipeline
import com.lmy.codec.pipeline.impl.GLEventPipeline
import com.lmy.codec.util.debug_e
import com.lmy.codec.util.debug_v
import com.lmy.codec.wrapper.CodecTextureWrapper


/**
 * Created by lmyooyo@gmail.com on 2018/3/28.
 */
class VideoEncoderImpl(var context: CodecContext,
                       private var textureId: IntArray,
                       private var eglContext: EGLContext,
                       override var onPreparedListener: Encoder.OnPreparedListener? = null,
                       asyn: Boolean = false)
    : Encoder {

    companion object {
        private const val WAIT_TIME = 10000L
    }

    private val outputFormatLock = Object()
    private val bufferInfo = MediaCodec.BufferInfo()
    private var codecWrapper: CodecTextureWrapper? = null
    private var codec: MediaCodec? = null
    private var mBufferInfo: MediaCodec.BufferInfo = MediaCodec.BufferInfo()
    private var pTimer: PresentationTimer = PresentationTimer(context.video.fps)
    override var onRecordListener: Encoder.OnRecordListener? = null
    private lateinit var format: MediaFormat
    private var mPipeline: Pipeline = if (asyn) {
        EventPipeline.create("VideoEncodePipeline")
    } else {
        GLEventPipeline.INSTANCE
    }
    private var mDequeuePipeline: Pipeline = if (GLEventPipeline.isMe(mPipeline)) {
        EventPipeline.create("VideoEncodePipeline")
    } else {
        mPipeline
    }
    private val mEncodingSyn = Any()
    private var mEncoding = false
    private var mFrameCount = 0
    private var onSampleListener: Encoder.OnSampleListener? = null
    private var nsecs: Long = Long.MIN_VALUE
    override fun setOnSampleListener(listener: Encoder.OnSampleListener) {
        onSampleListener = listener
    }

    init {
        initCodec()
        mPipeline.queueEvent({ init() })
    }

    private fun initCodec() {
        val f = CodecHelper.createVideoFormat(context)
        if (null == f) {
            debug_e("Unsupport codec type")
            return
        }
        format = f
        debug_v("create codec: ${format.getString(MediaFormat.KEY_MIME)}")
        try {
            codec = MediaCodec.createEncoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        } catch (e: Exception) {
            debug_e("Can not create codec")
        } finally {
            if (null == codec)
                debug_e("Can not create codec")
        }
    }

    private fun init() {
        if (null == codec) {
            debug_e("codec is null")
            return
        }
        pTimer.reset()
        codec!!.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codecWrapper = CodecTextureWrapper(codec!!.createInputSurface(), textureId, eglContext)
        codecWrapper?.egl?.makeCurrent()
        codec!!.start()
        onPreparedListener?.onPrepared(this)
    }

    override fun getOutputFormat(): MediaFormat {
        mPipeline.queueEvent({
            val index = codec!!.dequeueOutputBuffer(bufferInfo, WAIT_TIME)
            if (MediaCodec.INFO_OUTPUT_FORMAT_CHANGED != index) {
                getOutputFormat()
            } else {
                synchronized(outputFormatLock) {
                    outputFormatLock.notifyAll()
                }
            }
        })
        synchronized(outputFormatLock) {
            outputFormatLock.wait()
        }
        return codec!!.outputFormat
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        if (!mEncoding) return
        mPipeline.queueEvent({ encode() })
    }

    override fun setPresentationTime(nsecs: Long) {
        mPipeline.queueEvent({
            this.nsecs = nsecs * 1000
        })
    }

    private fun encode() {
        synchronized(mEncodingSyn) {
            pTimer.record()
            codecWrapper?.egl?.makeCurrent()
            GLES20.glViewport(0, 0, context.video.width, context.video.height)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glClearColor(0.3f, 0.3f, 0.3f, 0f)
            codecWrapper?.draw(null)
            codecWrapper?.egl?.setPresentationTime(if (Long.MIN_VALUE != nsecs)
                nsecs else pTimer.presentationTimeUs)
            codecWrapper?.egl?.swapBuffers()
            mDequeuePipeline.queueEvent(Runnable { dequeue() })
        }
    }

    /**
     * ??????OpenGL?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     */
    @SuppressLint("WrongConstant")
    private fun dequeue(): Boolean {
        try {
            /**
             * ??????????????????????????????Buffer?????????????????????
             * ?????????????????????????????????????????????????????????????????????WAIT_TIME??????????????????10000ms
             */
            val flag = codec!!.dequeueOutputBuffer(mBufferInfo, WAIT_TIME)
            when (flag) {
                MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {//Output buffer changes, usually ignored
                    debug_v("INFO_OUTPUT_BUFFERS_CHANGED")
                }
                MediaCodec.INFO_TRY_AGAIN_LATER -> {//Waiting timeout, need to wait again, usually ignored
//                  debug_v("INFO_TRY_AGAIN_LATER")
                    return false
                }
                /**
                 * ??????????????????????????????
                 * ???????????????outputFormat?????????MediaMuxer?????????????????????inputFormat?????????????????????????????????????????????????????????mp4??????
                 */
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    debug_v("INFO_OUTPUT_FORMAT_CHANGED")
                    onSampleListener?.onFormatChanged(this, codec!!.outputFormat)
                }
                else -> {
                    if (flag < 0) return false//If less than zero, skip
                    val buffer = codec!!.outputBuffers[flag]//Otherwise, the encoding is successful and the data can be taken out from the output buffer queue
                    if (null != buffer) {
                        //If the BUFFER_FLAG_END_OF_STREAM signal is not received, it means that the output data is valid
                        val endOfStream = mBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        if (endOfStream == 0)
                        {
                            if (mBufferInfo.size != 0) {
                                ++mFrameCount
                                buffer.position(mBufferInfo.offset)
                                buffer.limit(mBufferInfo.offset + mBufferInfo.size)
//                                mBufferInfo.presentationTimeUs = pTimer.presentationTimeUs
                                onSampleListener?.onSample(this, CodecHelper.copy(mBufferInfo), buffer)
                                onRecordListener?.onRecord(this, mBufferInfo.presentationTimeUs)
                            }
                        }
                        //After the buffer is used up, it must be returned to MediaCodec so that it can be used again.
                        //At this point, the process ends, and the cycle is repeated again.
                        codec!!.releaseOutputBuffer(flag, false)
//                        if (endOfStream == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
//                            return true
//                        }
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    override fun start() {
        synchronized(mEncodingSyn) {
            pTimer.start()
            mEncoding = true
        }
    }

    override fun pause() {
        synchronized(mEncodingSyn) {
            mEncoding = false
        }
    }

    override fun stop() {
        debug_e("Video encoder stop")
        pause()
        if (mFrameCount > 0) {
            while (dequeue()) {//??????????????????????????????
            }
            //???????????????????????????????????????surface??????????????????
            codec!!.signalEndOfInputStream()
        }
        this.nsecs = Long.MIN_VALUE
        mFrameCount = 0
        codec!!.stop()
        codec!!.release()
        mPipeline.queueEvent(Runnable {
            codecWrapper?.release()
            codecWrapper = null
        })
        if (!GLEventPipeline.isMe(mPipeline)) {
            mPipeline.quit()
        }
        if (!GLEventPipeline.isMe(mDequeuePipeline)) {
            mDequeuePipeline.quit()
        }
    }
}