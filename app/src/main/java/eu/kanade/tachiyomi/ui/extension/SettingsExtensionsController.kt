package eu.kanade.tachiyomi.ui.extension

import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.ui.setting.SettingsController
import eu.kanade.tachiyomi.ui.setting.onChange
import eu.kanade.tachiyomi.ui.setting.titleRes
import eu.kanade.tachiyomi.util.system.LocaleHelper
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SettingsExtensionsController : SettingsController() {

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = R.string.filter

        val activeLangs = preferences.enabledLanguages().get()

        val availableLangs =
            Injekt.get<ExtensionManager>().availableExtensions.groupBy {
                it.lang
            }.keys.minus("all").partition {
                it in activeLangs
            }.let {
                it.first + it.second
            }

        availableLangs.forEach {
            SwitchPreferenceCompat(context).apply {
                preferenceScreen.addPreference(this)
                title = LocaleHelper.getSourceDisplayName(it, context)
                isPersistent = false
                isChecked = it in activeLangs

                onChange { newValue ->
                    val checked = newValue as Boolean
                    val currentActiveLangs = preferences.enabledLanguages().get()

                    if (checked) {
                        preferences.enabledLanguages().set(currentActiveLangs + it)
                    } else {
                        preferences.enabledLanguages().set(currentActiveLangs - it)
                    }
                    true
                }
            }
        }
    }
}
