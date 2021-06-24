/*
 * Copyright (c) 2018-present, lmyooyo@gmail.com.
 *
 * This source code is licensed under the GPL license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.lmy.codec.texture.impl.filter

import com.lmy.codec.texture.IParams

/**
 * Created by lmyooyo@gmail.com on 2018/5/30.
 */
open class ColorMatrixFilter(width: Int = 0,
                             height: Int = 0,
                             textureId: IntArray = IntArray(1),
                             private var mIntensity: Float = 0f,
                             private var mColorMatrix: FloatArray = MATRIX)
    : BaseFilter(width, height, textureId) {

    private var aPositionLocation = 0
    private var uTextureLocation = 0
    private var aTextureCoordinateLocation = 0
    private var mColorMatrixLocation: Int = 0
    private var mIntensityLocation: Int = 0


    override fun init() {
        super.init()
        aPositionLocation = getAttribLocation("aPosition")
        uTextureLocation = getUniformLocation("uTexture")
        aTextureCoordinateLocation = getAttribLocation("aTextureCoord")
        mColorMatrixLocation = getUniformLocation("colorMatrix")
        mIntensityLocation = getUniformLocation("intensity")
    }

    override fun draw(transformMatrix: FloatArray?) {
        active(uTextureLocation)
        setUniform1f(mIntensityLocation, mIntensity)
        setUniformMatrix4fv(mColorMatrixLocation, mColorMatrix)
        enableVertex(aPositionLocation, aTextureCoordinateLocation)
        draw()
        disableVertex(aPositionLocation, aTextureCoordinateLocation)
        inactive()
    }

    override fun getVertex(): String {
        return "shader/vertex_normal.glsl"
    }

    override fun getFragment(): String {
        return "shader/fragment_color_matrix.glsl"
    }

    override fun setValue(index: Int, progress: Int) {
        when (index) {
            0 -> {
                setParams(floatArrayOf(
                        PARAM_INTENSITY, progress / 100f * 2,
                        IParams.PARAM_NONE
                ))
            }
        }
    }

    override fun setParam(cursor: Float, value: Float) {
        when {
            PARAM_INTENSITY == cursor -> this.mIntensity = value
        }
    }

    fun setColorMatrix(colorMatrix: FloatArray) {
        mColorMatrix = colorMatrix
    }

    companion object {
        const val PARAM_INTENSITY = 100f
        val MATRIX: FloatArray = floatArrayOf(
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f
        )
    }
}