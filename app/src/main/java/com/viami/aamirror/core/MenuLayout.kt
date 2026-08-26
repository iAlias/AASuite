package com.viami.aamirror.core

/** How the car home screen lists the modes. */
enum class MenuLayout(val key: String) {
    GRID("grid"),
    LIST("list");

    fun toggled(): MenuLayout = if (this == GRID) LIST else GRID

    companion object {
        fun fromKey(key: String?): MenuLayout =
            entries.firstOrNull { it.key == key } ?: GRID
    }
}
