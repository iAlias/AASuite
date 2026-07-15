package com.viami.aamirror.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.viami.aamirror.R
import com.viami.aamirror.browser.BrowserDisplay
import com.viami.aamirror.core.UrlResolver
import com.viami.aamirror.setup.BookmarkStore

/**
 * Entry point of the browser mode: the favorites saved from the phone app,
 * plus free search. Picking one opens the page on the car display.
 */
class BookmarksScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {

    init {
        lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        // Favorites may have been edited on the phone in the meantime.
        invalidate()
    }

    override fun onGetTemplate(): Template {
        val list = ItemList.Builder()
        list.addItem(
            Row.Builder()
                .setTitle(carContext.getString(R.string.row_search))
                .setImage(icon(R.drawable.ic_search))
                .setOnClickListener { openSearch() }
                .build()
        )
        BookmarkStore.load(carContext).forEach { bookmark ->
            list.addItem(
                Row.Builder()
                    .setTitle(bookmark.title)
                    .addText(bookmark.url)
                    .setImage(icon(R.drawable.ic_globe))
                    .setOnClickListener { open(bookmark.url) }
                    .build()
            )
        }
        return ListTemplate.Builder()
            .setSingleList(list.build())
            .setTitle(carContext.getString(R.string.menu_browser))
            .setHeaderAction(Action.BACK)
            .build()
    }

    private fun open(url: String) {
        BrowserDisplay.loadUrl(UrlResolver.resolve(url))
        screenManager.push(BrowserScreen(carContext))
    }

    private fun openSearch() {
        screenManager.push(
            SearchScreen(carContext) { query -> open(query) }
        )
    }

    private fun icon(iconRes: Int): CarIcon =
        CarIcon.Builder(IconCompat.createWithResource(carContext, iconRes)).build()
}
