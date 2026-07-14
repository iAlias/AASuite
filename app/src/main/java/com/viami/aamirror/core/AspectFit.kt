package com.viami.aamirror.core

data class FitRect(val x: Int, val y: Int, val width: Int, val height: Int)

object AspectFit {
    fun fit(srcWidth: Int, srcHeight: Int, dstWidth: Int, dstHeight: Int): FitRect {
        require(srcWidth > 0 && srcHeight > 0 && dstWidth > 0 && dstHeight > 0) {
            "dimensions must be positive: src=${srcWidth}x$srcHeight dst=${dstWidth}x$dstHeight"
        }
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
}
