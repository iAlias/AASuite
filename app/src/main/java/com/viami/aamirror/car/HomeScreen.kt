package com.viami.aamirror.car

import androidx.car.app.CarContext
import androidx.car.app.CarToast
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
import com.viami.aamirror.core.MirrorSettings
import com.viami.aamirror.input.RotationLock

/** Root menu of AA Suite: pick a mode or toggle the rotation lock. */
class HomeScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {

    init {
        lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        SurfaceRouter.register(carContext)
    }

    override fun onStart(owner: LifecycleOwner) {
        SurfaceRouter.setSink(null)
    }

    override fun onGetTemplate(): Template {
        val list = ItemList.Builder()
            .addItem(
                row(R.string.menu_mirror, R.drawable.ic_app) {
                    screenManager.push(MirrorScreen(carContext))
                }
            )
            .addItem(
                row(R.string.menu_browser, R.drawable.ic_globe) {
                    screenManager.push(BrowserScreen(carContext))
                }
            )
            .addItem(rotationRow())
            .addItem(fillRow())
            .build()
        return ListTemplate.Builder()
            .setSingleList(list)
            .setTitle(carContext.getString(R.string.app_name))
            .setHeaderAction(Action.APP_ICON)
            .build()
    }

    private fun rotationRow(): Row {
        val state = if (RotationLock.isLocked) {
            R.string.rotation_row_on
        } else {
            R.string.rotation_row_off
        }
        return Row.Builder()
            .setTitle(carContext.getString(R.string.menu_rotation))
            .addText(carContext.getString(state))
            .setImage(icon(R.drawable.ic_rotate))
            .setOnClickListener { toggleRotationLock() }
            .build()
    }

    private fun fillRow(): Row {
        val state = if (MirrorSettings.fillScreen) {
            R.string.fill_row_on
        } else {
            R.string.fill_row_off
        }
        return Row.Builder()
            .setTitle(carContext.getString(R.string.menu_fill))
            .addText(carContext.getString(state))
            .setImage(icon(R.drawable.ic_fill))
            .setOnClickListener {
                MirrorSettings.fillScreen = !MirrorSettings.fillScreen
                invalidate()
            }
            .build()
    }

    private fun toggleRotationLock() {
        if (!RotationLock.canLock(carContext)) {
            CarToast.makeText(
                carContext,
                carContext.getString(R.string.car_overlay_missing),
                CarToast.LENGTH_LONG,
            ).show()
            return
        }
        RotationLock.toggle(carContext)
        invalidate()
    }

    private fun row(titleRes: Int, iconRes: Int, onClick: () -> Unit): Row =
        Row.Builder()
            .setTitle(carContext.getString(titleRes))
            .setImage(icon(iconRes))
            .setOnClickListener(onClick)
            .build()

    private fun icon(iconRes: Int): CarIcon =
        CarIcon.Builder(IconCompat.createWithResource(carContext, iconRes)).build()
}
