package com.viami.aamirror.share

import kotlinx.coroutines.flow.MutableSharedFlow

/** Bridge between the share server thread and the car session. */
object ShareInbox {
    val links = MutableSharedFlow<String>(extraBufferCapacity = 4)

    fun publish(url: String) {
        links.tryEmit(url)
    }
}
