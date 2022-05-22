package it.polito.interdisciplinaryProjects2021.smartpresence.presenceDetection.manualBased

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import it.polito.interdisciplinaryProjects2021.smartpresence.R

const val shortcut_id_IN = "id_in"
const val shortcut_id_OUT = "id_out"

object Shortcuts {

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun setUp(context: Context) {
        val shortcutManager: ShortcutManager? =
            ContextCompat.getSystemService<ShortcutManager>(context, ShortcutManager::class.java)

        val intentCheckIn = Intent(Intent.ACTION_VIEW, null, context, CheckInActivity::class.java)
        val intentCheckOut = Intent(Intent.ACTION_VIEW, null, context, CheckOutActivity::class.java)

        val shortcutIn: ShortcutInfo = ShortcutInfo.Builder(context, shortcut_id_IN)
            .setShortLabel(context.getString(R.string.shortcutCheckInTitle))
            .setLongLabel(context.getString(R.string.shortcutCheckInLongTitle))
            .setIcon(Icon.createWithResource(context, R.drawable.ic_baseline_login_24_new))
            .setIntent(intentCheckIn)
            .build()
        val shortcutOut: ShortcutInfo = ShortcutInfo.Builder(context, shortcut_id_OUT)
            .setShortLabel(context.getString(R.string.shortcutCheckOutTitle))
            .setLongLabel(context.getString(R.string.shortcutCheckOutLongTitle))
            .setIcon(Icon.createWithResource(context, R.drawable.ic_baseline_logout_24_new))
            .setIntent(intentCheckOut)
            .build()

        shortcutManager!!.dynamicShortcuts = listOf(shortcutIn, shortcutOut)
    }

}