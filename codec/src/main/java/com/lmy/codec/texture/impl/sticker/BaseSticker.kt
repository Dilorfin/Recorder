/*
 * Copyright (c) 2018-present, lmyooyo@gmail.com.
 *
 * This source code is licensed under the GPL license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.lmy.codec.texture.impl.sticker

import android.graphics.Bitmap
import android.graphics.RectF
import android.opengl.GLES20
import android.opengl.GLUtils
import com.lmy.codec.entity.Size
import com.lmy.codec.helper.Resources
import com.lmy.codec.texture.impl.BaseTexture
import javax.microedition.khronos.opengles.GL10

abstract class BaseSticker(var width: Int,
                           var height: Int,
                           textureId: IntArray,
                           name: String = "BaseSticker") : BaseTexture(textureId, name) {
    private var aPositionLocation = 0
    private var aTextureCoordinateLocation = 0
    private var uTextureLocation = 0
    private var texture: IntArray = IntArray(1)

    override fun init() {
        super.init()
        if (width <= 0 || height <= 0) throw RuntimeException("Width and height cannot be 0")
        createProgram()
        createTexture(texture)
    }

    private fun createProgram() {
        shaderProgram = createProgram(Resources.instance.readAssetsAsString("shader/vertex_sticker.glsl"),
                Resources.instance.readAssetsAsString("shader/fragment_sticker.glsl"))
        aPositionLocation = getAttribLocation("aPosition")
        uTextureLocation = getUniformLocation("uTexture")
        aTextureCoordinateLocation = getAttribLocation("aTextureCoord")
    }

    fun createTexture(texture: IntArray) {
        GLES20.glGenTextures(texture.size, texture, 0)
        GLES20.glBindTexture(GL10.GL_TEXTURE_2D, texture[0])
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat())
        GLES20.glBindTexture(GL10.GL_TEXTURE_2D, GLES20.GL_NONE)
    }

    open fun updateSize(width: Int, height: Int) {
        this.width = width
        this.height = height
        val rect = getRect()
        updateLocation(floatArrayOf(
                0f, 1f,//LEFT,TOP
                1f, 1f,//RIGHT,TOP
                0f, 0f,//LEFT,BOTTOM
                1f, 0f//RIGHT,BOTTOM
        ), floatArrayOf(
                rect.left, rect.bottom,//LEFT,BOTTOM
                rect.right, rect.bottom,//RIGHT,BOTTOM
                rect.left, rect.top,//LEFT,TOP
                rect.right, rect.top//RIGHT,TOP
        ))
    }

    abstract fun getRect(): RectF

    fun bindTexture(bitmap: Bitmap) {
        GLES20.glBindTexture(GL10.GL_TEXTURE_2D, texture[0])
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES20.glBindTexture(GL10.GL_TEXTURE_2D, GLES20.GL_NONE)
        bitmap.recycle()
    }

    fun active() {
        GLES20.glUseProgram(shaderProgram!!)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, textureId[0])
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0])
        setUniform1i(uTextureLocation, 0)
        enableVertex(aPositionLocation, aTextureCoordinateLocation)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }

    protected fun draw() {
        drawer.draw()
    }

    fun inactive() {
        disableVertex(aPositionLocation, aTextureCoordinateLocation)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_NONE)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, GLES20.GL_NONE)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glUseProgram(GLES20.GL_NONE)
        GLES20.glFlush()
    }

    override fun release() {
        super.release()
        GLES20.glDeleteTextures(texture.size, texture, 0)
    }

    open class Sticker(var x: Float = 0f,
                       var y: Float = 0f,
                       var scale: Float = 1f,
                       internal var size: Size = Size(0, 0)) {

        fun getRect(width: Int, height: Int): RectF {
            val rect = RectF()
            rect.left = -1f
            rect.top = 1f
            rect.right = rect.left + size.width / width.toFloat() * scale
            rect.bottom = rect.top - size.height / height.toFloat() * scale
            rect.offset(x * 2, -y * 2)
            return rect
        }
    }
}