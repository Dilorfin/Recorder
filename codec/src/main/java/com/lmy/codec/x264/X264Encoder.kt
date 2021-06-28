/*
 * Copyright (c) 2018-present, lmyooyo@gmail.com.
 *
 * This source code is licensed under the GPL license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.lmy.codec.x264

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Build
import com.lmy.codec.helper.Libyuv
import com.lmy.codec.util.debug_e
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Created by lmyooyo@gmail.com on 2018/4/3.
 */
class X264Encoder(private var format: MediaFormat,
                  val colorFormat: Int = Libyuv.COLOR_RGBA,
                  var buffer: ByteBuffer? = null,
                  private var size: IntArray = IntArray(1),
                  private var type: IntArray = IntArray(1),
                  private var ppsLength: Int = 0,
                  private var outFormat: MediaFormat? = null,
                  private var mBufferInfo: MediaCodec.BufferInfo = MediaCodec.BufferInfo()) : X264 {

    companion object {
        private const val PRESET = "veryfast"
        private const val TUNE = "zerolatency"

        private val CSD_0 = "csd-0"
        private val CSD_1 = "csd-1"

        const val BUFFER_FLAG_KEY_FRAME = 1
        const val BUFFER_FLAG_CODEC_CONFIG = 2
        const val BUFFER_FLAG_END_OF_STREAM = 4
        const val BUFFER_FLAG_PARTIAL_FRAME = 8
    }

    private var mTotalCost = 0L
    private var mFrameCount = 0

    init {
        System.loadLibrary("x264")
        System.loadLibrary("codec")
        initCacheBuffer()
        init(colorFormat)
        setVideoSize(getWidth(), getHeight())
        setBitrate(format.getInteger(MediaFormat.KEY_BIT_RATE))
        setFrameFormat(FrameFormat.X264_CSP_I420)
        setFps(format.getInteger(MediaFormat.KEY_FRAME_RATE))
    }

    private fun wrapBufferInfo(size: Int): MediaCodec.BufferInfo? {
        when (type[0]) {
            -1 -> mBufferInfo.flags = BUFFER_FLAG_CODEC_CONFIG
            1 -> mBufferInfo.flags = BUFFER_FLAG_KEY_FRAME//X264_TYPE_IDR
            2 -> mBufferInfo.flags = BUFFER_FLAG_KEY_FRAME//X264_TYPE_I
            else -> mBufferInfo.flags = 0
        }
        mBufferInfo.size = size

        if (BUFFER_FLAG_CODEC_CONFIG == mBufferInfo.flags) {
            //获取SPS，PPS
            getOutFormat(mBufferInfo, buffer!!)
        } else {
            buffer!!.position(0)
            buffer!!.limit(size)
        }
        return mBufferInfo
    }

    private fun getOutFormat(info: MediaCodec.BufferInfo, data: ByteBuffer) {
        data.position(0)
        val specialData = ByteArray(info.size)
        data.get(specialData, 0, specialData!!.size)
        outFormat = MediaFormat()
        outFormat?.setString(MediaFormat.KEY_MIME, format.getString(MediaFormat.KEY_MIME))
        outFormat?.setInteger(MediaFormat.KEY_WIDTH, format.getInteger(MediaFormat.KEY_WIDTH))
        outFormat?.setInteger(MediaFormat.KEY_HEIGHT, format.getInteger(MediaFormat.KEY_HEIGHT))
        outFormat?.setInteger(MediaFormat.KEY_BIT_RATE, format.getInteger(MediaFormat.KEY_BIT_RATE))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            outFormat?.setInteger(MediaFormat.KEY_COLOR_RANGE, 2)
            outFormat?.setInteger(MediaFormat.KEY_COLOR_STANDARD, 4)
            outFormat?.setInteger(MediaFormat.KEY_COLOR_TRANSFER, 3)
        }
        val spsAndPps = parseSpecialData(specialData)
                ?: throw RuntimeException("Special data is empty")
        ppsLength = spsAndPps[0].size
        outFormat?.setByteBuffer(CSD_0, ByteBuffer.wrap(spsAndPps[0]))
        outFormat?.setByteBuffer(CSD_1, ByteBuffer.wrap(spsAndPps[1]))
    }

    private fun parseSpecialData(specialData: ByteArray): Array<ByteArray>? {
        val index = (4 until specialData.size - 4).firstOrNull { isFlag(specialData, it) }
                ?: 0
        if (0 == index) return null
        return arrayOf(specialData.copyOfRange(0, index),
                specialData.copyOfRange(index, specialData.size))
    }

    private fun isFlag(specialData: ByteArray, index: Int): Boolean {
        return 0 == specialData[index].toInt()
                && 0 == specialData[index + 1].toInt()
                && 0 == specialData[index + 2].toInt()
                && 1 == specialData[index + 3].toInt()
    }

    override fun encode(src: ByteArray): MediaCodec.BufferInfo? {
        val time = System.currentTimeMillis()
        ++mFrameCount
        buffer?.clear()
        buffer?.position(0)
        val result = encode(src, buffer!!.array(), size, type, colorFormat)
        if (!result) {
            debug_e("Encode failed. size = ${size[0]}")
            return null
        }
        val cost = System.currentTimeMillis() - time
        mTotalCost += cost
        if (0 == mFrameCount % 20)
            debug_e("x264 frame size = ${size[0]}, cost ${cost}ms, arg cost ${mTotalCost / mFrameCount}ms")
        return wrapBufferInfo(size[0])
    }

    fun getOutFormat(): MediaFormat {
        return outFormat!!
    }

    /**
     * 注意：返回一个与buffer共享内存的子buffer，子buffer的array()将返回整个buffer的内存空间。
     *
     * NOTE: Returns a subbuffer that shares memory with the buffer.
     * The subarray's array() will return the entire buffer's memory space.
     */
    fun getOutBuffer(): ByteBuffer {
        if (null == buffer) {
            throw RuntimeException("Please init buffer!")
        }
        buffer!!.position(0)
        buffer!!.limit(mBufferInfo.size)
        return buffer!!.slice()
    }

    /**
     * 初始化缓存，大小为width*height
     * 如果是别的编码格式，缓存大小可能需要增大
     */
    private fun initCacheBuffer() {
        buffer = ByteBuffer.allocate(getWidth() * getHeight())
        buffer?.order(ByteOrder.nativeOrder())
    }

    override fun getWidth(): Int {
        return format.getInteger(MediaFormat.KEY_WIDTH)
    }

    override fun getHeight(): Int {
        return format.getInteger(MediaFormat.KEY_HEIGHT)
    }

    override fun release() {
        stop()
        buffer = null
        outFormat = null
    }

    override fun post(event: Runnable): X264 {
        event.run()
        return this
    }

    private external fun init(fmt: Int)
    external override fun start()
    external override fun stop()
    external fun encode(src: ByteArray, dest: ByteArray, size: IntArray, type: IntArray, fmt: Int): Boolean
    external fun setVideoSize(width: Int, height: Int)
    external fun setBitrate(bitrate: Int)
    external fun setFrameFormat(format: Int)
    external fun setFps(fps: Int)
    external override fun setProfile(profile: String)
    external override fun setLevel(level: Int)
}