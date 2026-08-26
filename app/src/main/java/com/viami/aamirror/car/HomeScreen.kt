package com.viami.aamirror.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.viami.aamirror.R
import com.viami.aamirror.core.MenuLayout
import com.viami.aamirror.core.MirrorSettings
import com.viami.aamirror.setup.SettingsStore

/** Root menu of AA Suite: pick a mode, or open the settings. */
class HomeScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {

    /**
     * One entry of the menu. Grid cells truncate long titles, so they carry
     * a short label of their own.
     */
    private class Mode(
        val titleRes: Int,
        val shortTitleRes: Int,
        val iconRes: Int,
        val open: () -> Unit,
    )

    private val modes = listOf(
        Mode(R.string.menu_mirror, R.string.menu_mirror_short, R.drawable.ic_app) {
            screenManager.push(MirrorScreen(carContext))
        },
        Mode(R.string.menu_browser, R.string.menu_browser_short, R.drawable.ic_globe) {
            screenManager.push(BookmarksScreen(carContext))
        },
        Mode(R.string.menu_youtube, R.string.menu_youtube_short, R.drawable.ic_cast) {
            screenManager.push(YouTubeScreen(carContext))
        },
    )

    init {
        lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        SurfaceRouter.register(carContext)
        SettingsStore.load(carContext)
    }

    override fun onStart(owner: LifecycleOwner) {
        SurfaceRouter.setSink(null)
        // The settings screen may have changed the layout while we were away.
        invalidate()
    }

    override fun onGetTemplate(): Template =
        if (MirrorSettings.menuLayout == MenuLayout.GRID) gridTemplate() else listTemplate()

    private fun gridTemplate(): Template {
        val items = ItemList.Builder()
        for (mode in modes) {
            items.addItem(
                GridItem.Builder()
                    .setTitle(carContext.getString(mode.shortTitleRes))
                    .setImage(icon(mode.iconRes))
                    .setOnClickListener(mode.open)
                    .build()
            )
        }
        return GridTemplate.Builder()
            .setSingleList(items.build())
            .setTitle(carContext.getString(R.string.app_name))
            .setHeaderAction(Action.APP_ICON)
            .setActionStrip(settingsStrip())
            .build()
    }

    private fun listTemplate(): Template {
        val items = ItemList.Builder()
        for (mode in modes) {
            items.addItem(
                Row.Builder()
                    .setTitle(carContext.getString(mode.titleRes))
                    .setImage(icon(mode.iconRes))
                    .setOnClickListener(mode.open)
                    .build()
            )
        }
        return ListTemplate.Builder()
            .setSingleList(items.build())
            .setTitle(carContext.getString(R.string.app_name))
            .setHeaderAction(Action.APP_ICON)
            .setActionStrip(settingsStrip())
            .build()
    }

    private fun settingsStrip(): ActionStrip =
        ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setIcon(icon(R.drawable.ic_settings))
                    .setOnClickListener { screenManager.push(SettingsScreen(carContext)) }
                    .build()
            )
            .build()

    private fun icon(iconRes: Int): CarIcon =
        CarIcon.Builder(IconCompat.createWithResource(carContext, iconRes)).build()
}
