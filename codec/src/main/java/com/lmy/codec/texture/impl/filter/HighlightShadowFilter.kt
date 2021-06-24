/*
 * Copyright (c) 2018-present, lmyooyo@gmail.com.
 *
 * This source code is licensed under the GPL license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.lmy.codec.texture.impl.filter

import com.lmy.codec.texture.IParams

/**
 * 高光阴影
 * Created by lmyooyo@gmail.com on 2018/5/30.
 */
class HighlightShadowFilter(width: Int = 0,
                            height: Int = 0,
                            textureId: IntArray = IntArray(1),
                            private var mShadows: Float = 0f,
                            private var mHighlights: Float = 0f) : BaseFilter(width, height, textureId) {

    private var aPositionLocation = 0
    private var uTextureLocation = 0
    private var aTextureCoordinateLocation = 0
    private var mShadowsLocation = 0
    private var mHighlightsLocation = 0


    override fun init() {
        super.init()
        aPositionLocation = getAttribLocation("aPosition")
        uTextureLocation = getUniformLocation("uTexture")
        aTextureCoordinateLocation = getAttribLocation("aTextureCoord")
        mShadowsLocation = getUniformLocation("shadows")
        mHighlightsLocation = getUniformLocation("highlights")
    }

    override fun draw(transformMatrix: FloatArray?) {
        active(uTextureLocation)
        setUniform1f(mHighlightsLocation, mHighlights)
        setUniform1f(mShadowsLocation, mShadows)
        enableVertex(aPositionLocation, aTextureCoordinateLocation)
        draw()
        disableVertex(aPositionLocation, aTextureCoordinateLocation)
        inactive()
    }

    override fun getVertex(): String {
        return "shader/vertex_normal.glsl"
    }

    override fun getFragment(): String {
        return "shader/fragment_highlight_shadow.glsl"
    }

    /**
     * 0 == index: Highlights
     * 1 == index: Shadows
     */
    override fun setValue(index: Int, progress: Int) {
        when (index) {
            0 -> {
                setParams(floatArrayOf(
                        PARAM_HIGHLIGHTS, progress / 100f * 2,
                        IParams.PARAM_NONE
                ))
            }
            1 -> {
                setParams(floatArrayOf(
                        PARAM_SHADOWS, progress / 100f * 2,
                        IParams.PARAM_NONE
                ))
            }
        }
    }

    override fun setParam(cursor: Float, value: Float) {
        when {
            PARAM_HIGHLIGHTS == cursor -> this.mHighlights = value
            PARAM_SHADOWS == cursor -> this.mShadows = value
        }
    }

    companion object {
        const val PARAM_HIGHLIGHTS = 100f
        const val PARAM_SHADOWS = PARAM_HIGHLIGHTS + 1
    }
}