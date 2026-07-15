package com.viami.aamirror.core

data class FitRect(val x: Int, val y: Int, val width: Int, val height: Int)

object AspectFit {
    fun fit(srcWidth: Int, srcHeight: Int, dstWidth: Int, dstHeight: Int): FitRect {
        requirePositive(srcWidth, srcHeight, dstWidth, dstHeight)
        val scale = minOf(
            dstWidth.toFloat() / srcWidth,
            dstHeight.toFloat() / srcHeight,
        )
        val width = (srcWidth * scale).toInt()
        val height = (srcHeight * scale).toInt()
        return FitRect(
            x = (dstWidth - width) / 2,
            y = (dstHeight - height) / 2,
            width = width,
            height = height,
        )
    }

    /**
     * The centered sub-rectangle OF THE SOURCE that has the destination's
     * aspect ratio: drawing it scaled onto the full destination fills the
     * screen, cropping what falls outside ("center crop").
     */
    fun sourceCrop(srcWidth: Int, srcHeight: Int, dstWidth: Int, dstHeight: Int): FitRect {
        requirePositive(srcWidth, srcHeight, dstWidth, dstHeight)
        val srcAspect = srcWidth.toFloat() / srcHeight
        val dstAspect = dstWidth.toFloat() / dstHeight
        val width: Int
        val height: Int
        if (srcAspect > dstAspect) {
            height = srcHeight
            width = (srcHeight * dstAspect).toInt()
        } else {
            width = srcWidth
            height = (srcWidth / dstAspect).toInt()
        }
        return FitRect(
            x = (srcWidth - width) / 2,
            y = (srcHeight - height) / 2,
            width = width,
            height = height,
        )
    }

    private fun requirePositive(srcWidth: Int, srcHeight: Int, dstWidth: Int, dstHeight: Int) {
        require(srcWidth > 0 && srcHeight > 0 && dstWidth > 0 && dstHeight > 0) {
            "dimensions must be positive: src=${srcWidth}x$srcHeight dst=${dstWidth}x$dstHeight"
        }
    }
}
