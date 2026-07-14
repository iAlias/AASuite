package com.viami.aamirror.core

data class PhonePoint(val x: Float, val y: Float)

object TouchMapper {
    /**
     * Maps a tap on the car surface into phone-screen coordinates by
     * inverting the aspect-fit mapping. Returns null when the tap falls
     * on the letterbox bars (outside [content]).
     */
    fun mapTap(
        carX: Float,
        carY: Float,
        content: FitRect,
        phoneWidth: Int,
        phoneHeight: Int,
    ): PhonePoint? {
        if (carX < content.x || carX >= content.x + content.width) return null
        if (carY < content.y || carY >= content.y + content.height) return null
        return PhonePoint(
            x = (carX - content.x) / content.width * phoneWidth,
            y = (carY - content.y) / content.height * phoneHeight,
        )
    }
}
