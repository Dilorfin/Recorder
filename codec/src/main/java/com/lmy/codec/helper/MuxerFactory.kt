package com.lmy.codec.helper

import android.text.TextUtils
import com.lmy.codec.entity.CodecContext
import com.lmy.codec.muxer.Muxer
import com.lmy.codec.muxer.impl.MuxerImpl
import com.lmy.codec.muxer.impl.RtmpMuxerImpl

/**
 * Created by lmyooyo@gmail.com on 2018/7/25.
 */
class MuxerFactory {
    companion object {
        fun getMuxer(context: CodecContext): Muxer {
            if (TextUtils.isEmpty(context.ioContext.path)) {
                throw RuntimeException("context.ioContext.path can not be null!")
            }
            return if (context.ioContext.path!!.startsWith("rtmp")) {
                RtmpMuxerImpl(context)
            } else {
                MuxerImpl(context)
            }
        }
    }
}