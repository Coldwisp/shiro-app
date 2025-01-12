/*
 * Copyright (C) 2018 Jared Rummler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lagradost.shiro.ui.settings

import android.content.Intent
import android.os.Build
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.annotation.XmlRes
import androidx.preference.*
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.Preference.OnPreferenceClickListener
import com.jaredrummler.android.colorpicker.ColorPreferenceCompat
import com.jaredrummler.cyanea.Cyanea
import com.jaredrummler.cyanea.app.BaseCyaneaActivity
import com.jaredrummler.cyanea.prefs.CyaneaThemePickerActivity
import com.jaredrummler.cyanea.prefs.CyaneaThemePickerLauncher
import com.jaredrummler.cyanea.tinting.SystemBarTint
import com.jaredrummler.cyanea.utils.ColorUtils
import com.lagradost.shiro.R
import com.lagradost.shiro.ui.MainActivity.Companion.statusHeight
import com.lagradost.shiro.utils.AppUtils.changeStatusBarState
import com.lagradost.shiro.utils.AppUtils.getCurrentActivity

/**
 * Fragment to display preferences to modify the primary, accent, and background color of the app.
 */
open class CyaneaSettingsFragment : PreferenceFragmentCompat(), OnPreferenceChangeListener, OnPreferenceClickListener {

    private lateinit var prefThemePicker: Preference
    private lateinit var prefColorPrimary: ColorPreferenceCompat
    private lateinit var prefColorAccent: ColorPreferenceCompat
    private lateinit var prefColorBackground: ColorPreferenceCompat
    private lateinit var prefColorNavBar: SwitchPreferenceCompat

    /**
     * The [Cyanea] instance used for styling.
     */
    open val cyanea: Cyanea get() = (activity as? BaseCyaneaActivity)?.cyanea ?: Cyanea.instance

    /**
     * Get the preferences resource to load into the preference hierarchy.
     *
     * The preferences should contain a [ColorPreferenceCompat] for "pref_color_primary",
     * "pref_color_accent" and "pref_color_background".
     *
     * It should also contain preferences for "pref_theme_picker" and "pref_color_navigation_bar".
     *
     * @return The XML resource id to inflate
     */
    @XmlRes
    open fun getPreferenceXmlResId(): Int = R.xml.custom_pref_cyanea

    /**
     * Sets whether to reserve the space of all Preference views. If set to false, all padding will be removed.
     *
     * By default, if the action bar is displaying home as up then padding will be added to the preference.
     */
    open val iconSpaceReserved = false
//    get() = (activity as? AppCompatActivity)?.supportActionBar?.displayOptions?.and(ActionBar.DISPLAY_HOME_AS_UP) != 0

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(getPreferenceXmlResId(), rootKey)

        prefThemePicker = findPreference(PREF_THEME_PICKER)
        prefColorPrimary = findPreference(PREF_COLOR_PRIMARY)
        prefColorAccent = findPreference(PREF_COLOR_ACCENT)
        prefColorBackground = findPreference(PREF_COLOR_BACKGROUND)
        //prefColorNavBar = findPreference(PREF_COLOR_NAV_BAR)

        prefColorPrimary.saveValue(cyanea.primary)
        prefColorAccent.saveValue(cyanea.accent)
        prefColorBackground.saveValue(cyanea.backgroundColor)

        prefThemePicker.onPreferenceClickListener = this
        prefColorPrimary.onPreferenceChangeListener = this
        prefColorAccent.onPreferenceChangeListener = this
        prefColorBackground.onPreferenceChangeListener = this
        //prefColorNavBar.onPreferenceChangeListener = this

        /** Removed nav bar color selection */
        //setupNavBarPref()

        getCurrentActivity()!!.title = "Style settings"

        val statusBarHidden = findPreference<SwitchPreference?>("statusbar_hidden")
        statusBarHidden?.setOnPreferenceChangeListener { _, newValue ->
            activity?.changeStatusBarState(newValue == true)?.let {
                statusHeight = it
            }
            return@setOnPreferenceChangeListener true
        }

    }

    override fun onPreferenceClick(preference: Preference?): Boolean {
        return when (preference) {
            prefThemePicker -> {
                activity?.run {
                    if (this is CyaneaThemePickerLauncher) {
                        launchThemePicker()
                    } else {
                        startActivity(Intent(this, CyaneaThemePickerActivity::class.java))
                    }
                }
                true
            }
            else -> false
        }
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        fun editTheme(action: (editor: Cyanea.Editor) -> Unit) {
            cyanea.edit {
                action(this)
            }.recreate(requireActivity(), smooth = true)
        }

        when (preference) {
            prefColorPrimary -> editTheme { it.primary(newValue as Int) }
            prefColorAccent -> editTheme { it.accent(newValue as Int) }
            prefColorBackground -> editTheme { it.background(newValue as Int) }
            prefColorNavBar -> editTheme { it.shouldTintNavBar(newValue as Boolean) }
            else -> return false
        }

        return true
    }

    private fun setupNavBarPref() {
        ColorUtils.isDarkColor(cyanea.primary, 0.75).let { isDarkEnough ->
            prefColorNavBar.isEnabled = isDarkEnough || VERSION.SDK_INT >= VERSION_CODES.O
        }
        val isColored = if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            activity?.window?.navigationBarColor == cyanea.primary
        } else false
        prefColorNavBar.isChecked = cyanea.shouldTintNavBar || isColored
        val sysBarConfig = SystemBarTint(requireActivity()).sysBarConfig
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT || !sysBarConfig.hasNavigationBar) {
            findPreference<PreferenceCategory>(PREF_CATEGORY).run {
                removePreference(prefColorNavBar)
            }
        }
    }

    private inline fun <reified T : Preference> findPreference(key: String): T =
        super.findPreference<Preference>(key) as T

    companion object {
        private const val PREF_CATEGORY = "cyanea_preference_category"
        private const val PREF_THEME_PICKER = "pref_theme_picker"
        private const val PREF_COLOR_PRIMARY = "pref_color_primary"
        private const val PREF_COLOR_ACCENT = "pref_color_accent"
        private const val PREF_COLOR_BACKGROUND = "pref_color_background"
        //private const val PREF_COLOR_NAV_BAR = "pref_color_navigation_bar"

        fun newInstance() = CyaneaSettingsFragment()
    }

}

/**
 * Let the hosting activity implement this to launch a custom theme picker from preferences
 */
interface CyaneaThemePickerLauncher {

    /**
     * Launch a theme picker for Cyanea
     */
    fun launchThemePicker()
}
