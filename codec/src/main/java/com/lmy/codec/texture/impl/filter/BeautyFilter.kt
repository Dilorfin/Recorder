/*
 * Copyright (c) 2018-present, lmyooyo@gmail.com.
 *
 * This source code is licensed under the GPL license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.lmy.codec.texture.impl.filter

import com.lmy.codec.texture.IParams

/**
 * Created by lmyooyo@gmail.com on 2018/5/29.
 */
class BeautyFilter(width: Int = 0,
                   height: Int = 0,
                   textureId: IntArray = IntArray(1)) : BaseFilter(width, height, textureId) {

    private var aPositionLocation = 0
    private var aTextureCoordinateLocation = 0
    private var uTextureLocation = 0

    private var paramsLocation = 0
    private var brightnessLocation = 0
    private var texelWidthLocation = 0
    private var texelHeightLocation = 0

    override fun init() {
        super.init()
        aPositionLocation = getAttribLocation("aPosition")
        uTextureLocation = getUniformLocation("uTexture")
        aTextureCoordinateLocation = getAttribLocation("aTextureCoord")
        //美颜参数
        paramsLocation = getUniformLocation("params")
        brightnessLocation = getUniformLocation("brightness")
        texelWidthLocation = getUniformLocation("texelWidthOffset")
        texelHeightLocation = getUniformLocation("texelHeightOffset")
    }

    override fun draw(transformMatrix: FloatArray?) {
        active(uTextureLocation)
        setRgba(rgba)
        setBrightLevel(brightLevel)
        setTexelOffset(texelOffset)
        enableVertex(aPositionLocation, aTextureCoordinateLocation)
        draw()
        disableVertex(aPositionLocation, aTextureCoordinateLocation)
        inactive()
    }

    override fun getVertex(): String {
        return "shader/vertex_beauty.glsl"
    }

    override fun getFragment(): String {
        return "shader/fragment_beauty.glsl"
    }

    private var brightLevel = 0.55f
    private var texelOffset = 0.5f
    private var rgba = floatArrayOf(0.33f, 0.63f, 0.4f, 0.15f)

    /**
     * 0==index: bright/亮度
     * 1==index: texelOffset/磨皮
     * 2==index: rosy/红润
     */
    override fun setValue(index: Int, progress: Int) {
        when (index) {
            0 -> {
                setParams(floatArrayOf(
                        PARAM_BRIGHT, progress / 100f,
                        IParams.PARAM_NONE
                ))
            }
            1 -> {
                setParams(floatArrayOf(
                        PARAM_TEXEL_OFFSET, progress / 100f * 2,
                        IParams.PARAM_NONE
                ))
            }
            2 -> {
                setParams(floatArrayOf(
                        PARAM_ROSY, progress / 100f,
                        IParams.PARAM_NONE
                ))
            }
        }
    }

    override fun setParam(cursor: Float, value: Float) {
        when {
            PARAM_BRIGHT == cursor -> this.brightLevel = value
            PARAM_TEXEL_OFFSET == cursor -> this.texelOffset = value
            PARAM_ROSY == cursor -> this.rgba[3] = value
        }
    }

    /**
     * 0 - 1
     */
    private fun setBrightLevel(brightLevel: Float) {
        setUniform1f(brightnessLocation, 0.6f * (-0.5f + brightLevel))
    }

    /**
     * beauty: 0 - 2.5, tone: -5 - 5
     */
    private fun setRgba(rgba: FloatArray) {
        setUniform4fv(paramsLocation, rgba)
    }

    /**
     * -1 - 1
     */
    private fun setTexelOffset(texelOffset: Float) {
        setUniform1f(texelWidthLocation, texelOffset / width)
        setUniform1f(texelHeightLocation, texelOffset / height)
    }

    companion object {
        const val PARAM_BRIGHT = 100f
        const val PARAM_TEXEL_OFFSET = PARAM_BRIGHT + 1
        const val PARAM_ROSY = PARAM_BRIGHT + 2
    }
}