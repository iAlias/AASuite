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
import com.viami.aamirror.R
import com.viami.aamirror.browser.BrowserDisplay
import com.viami.aamirror.browser.YouTubeDisplay
import com.viami.aamirror.core.MenuLayout
import com.viami.aamirror.core.MirrorSettings
import com.viami.aamirror.input.RotationLock
import com.viami.aamirror.setup.SettingsStore

/** Everything that is a preference rather than a mode. */
class SettingsScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val list = ItemList.Builder()
            .addItem(layoutRow())
            .addItem(rotationRow())
            .addItem(fillRow())
            .build()
        return ListTemplate.Builder()
            .setSingleList(list)
            .setTitle(carContext.getString(R.string.settings_title))
            .setHeaderAction(Action.BACK)
            .build()
    }

    private fun layoutRow(): Row {
        val state = if (MirrorSettings.menuLayout == MenuLayout.GRID) {
            R.string.layout_row_grid
        } else {
            R.string.layout_row_list
        }
        return row(R.string.settings_layout, state, R.drawable.ic_grid) {
            MirrorSettings.menuLayout = MirrorSettings.menuLayout.toggled()
            SettingsStore.save(carContext)
            invalidate()
        }
    }

    private fun rotationRow(): Row {
        val state = if (RotationLock.isLocked) {
            R.string.rotation_row_on
        } else {
            R.string.rotation_row_off
        }
        return row(R.string.menu_rotation, state, R.drawable.ic_rotate) {
            toggleRotationLock()
        }
    }

    private fun fillRow(): Row {
        val state = if (MirrorSettings.fillScreen) {
            R.string.fill_row_on
        } else {
            R.string.fill_row_off
        }
        return row(R.string.menu_fill, state, R.drawable.ic_fill) {
            MirrorSettings.fillScreen = !MirrorSettings.fillScreen
            SettingsStore.save(carContext)
            // The web modes letterbox or crop their video to match.
            BrowserDisplay.refreshVideoFit()
            YouTubeDisplay.refreshVideoFit()
            invalidate()
        }
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

    private fun row(titleRes: Int, stateRes: Int, iconRes: Int, onClick: () -> Unit): Row =
        Row.Builder()
            .setTitle(carContext.getString(titleRes))
            .addText(carContext.getString(stateRes))
            .setImage(
                CarIcon.Builder(IconCompat.createWithResource(carContext, iconRes)).build()
            )
            .setOnClickListener(onClick)
            .build()
}
