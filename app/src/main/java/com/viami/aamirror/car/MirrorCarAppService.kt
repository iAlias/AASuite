package com.viami.aamirror.car

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

/**
 * ALLOW_ALL_HOSTS_VALIDATOR is acceptable for a personal sideloaded app;
 * a Play-Store app would pin the official host signatures.
 */
class MirrorCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator =
        HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = MirrorSession()
}
