package com.viami.aamirror.car

import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.navigation.model.NavigationTemplate

/**
 * The host forwards drags on the surface to SurfaceCallback.onScroll only
 * when the template offers pan: Action.PAN has to sit in the map action
 * strip, and a pan-mode listener must be set for the host to enable it.
 */
fun NavigationTemplate.Builder.withPan(): NavigationTemplate.Builder =
    setMapActionStrip(ActionStrip.Builder().addAction(Action.PAN).build())
        .setPanModeListener { }
