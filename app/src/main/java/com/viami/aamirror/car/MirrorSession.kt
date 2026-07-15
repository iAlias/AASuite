package com.viami.aamirror.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import com.viami.aamirror.browser.BrowserDisplay
import com.viami.aamirror.core.UrlResolver
import com.viami.aamirror.share.ShareInbox
import com.viami.aamirror.share.ShareServer
import kotlinx.coroutines.launch

class MirrorSession : Session(), DefaultLifecycleObserver {

    private val shareServer = ShareServer(onUrl = ShareInbox::publish)

    init {
        lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        owner.lifecycle.coroutineScope.launch {
            ShareInbox.links.collect { url -> openInBrowser(url) }
        }
        shareServer.start()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        shareServer.stop()
    }

    private fun openInBrowser(url: String) {
        BrowserDisplay.loadUrl(UrlResolver.resolve(url))
        val manager = carContext.getCarService(ScreenManager::class.java)
        manager.popToRoot()
        manager.push(BrowserScreen(carContext))
    }

    override fun onCreateScreen(intent: Intent): Screen = HomeScreen(carContext)
}
