package ru.runa.wfe.ui.notification

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.launch
import ru.runa.wfe.data.PreferencesManager
import ru.runa.wfe.R
import ru.runa.wfe.notification.NotificationLogic.NotificationType
import ru.runa.wfe.ui.fragments.DurationPreferenceDialogFragmentCompat

class NotificationSettingsFragment : PreferenceFragmentCompat() {
    private lateinit var preferencesManager: PreferencesManager

    private var soundUri: Uri? = null
    private var lastPickedSoundKey = ""

    private val ringtonePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    soundUri = result.data?.getParcelableExtra(
                        RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                        Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    soundUri = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                }
                preferenceManager.sharedPreferences?.edit {
                    putString(lastPickedSoundKey, soundUri.toString())
                }
            }

            else -> {
                Toast.makeText(
                    context,
                    this.getString(R.string.settings_value_error_message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun checkDelaySummary(number: Int): String {
        return "$number ${resources.getQuantityString(R.plurals.minutes, number)}"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.notification_settings, rootKey)
        preferencesManager = PreferencesManager(requireContext())

        // Set custom preference and it's summary
        val checkDelay: DurationPreference? = findPreference("checkDelay")
        checkDelay?.summary = checkDelay?.duration?.let { checkDelaySummary(it) }

        lifecycleScope.launch {
            if (!preferencesManager.hasKey(PreferencesManager.CHECK_DELAY)) {
                preferencesManager.setKey(
                    PreferencesManager.CHECK_DELAY,
                    DurationPreference.DEFAULT
                )
            }
        }

        findPreference<DurationPreference>("checkDelay")
            ?.setOnPreferenceChangeListener { _, newValue ->
                val newCheckDelayValue = newValue.toString().toIntOrNull()
                val currentCheckDelay = preferencesManager.getValue(PreferencesManager.CHECK_DELAY, 3)
                newCheckDelayValue?.let {
                    if (newCheckDelayValue != currentCheckDelay) {
                        lifecycleScope.launch {
                            preferencesManager.setKey(
                                PreferencesManager.CHECK_DELAY,
                                newCheckDelayValue
                            )
                        }
                        checkDelay?.let {
                            it.summary = checkDelaySummary(newCheckDelayValue)
                        }
                    }
                }
                true
            }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is DurationPreference) {
            val dialogFragment: DialogFragment =
                DurationPreferenceDialogFragmentCompat.newInstance(preference.key)
            // Currently (March of 2026) setTargetFragment must be called despite the deprecation
            @Suppress("DEPRECATION")
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(parentFragmentManager, null)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    private fun runRingtonePicker(title: String, key: String) {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, title)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, soundUri)
        ringtonePickerLauncher.launch(intent)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        val key = preference.key
        if (key.equals("tasksSound") || key.equals("messagesSound")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                showNotificationSettingsOreo(key)
            } else {
                // To show settings screen on old versions
                // showNotificationSettingsBelowOreo()

                // Show custom ringtone picker on old versions
                when (key) {
                    "tasksSound" -> {
                        runRingtonePicker(preference.title.toString(), key)
                        lastPickedSoundKey = key
                        return true
                    }

                    "messagesSound" -> {
                        runRingtonePicker(preference.title.toString(), key)
                        lastPickedSoundKey = key
                        return true
                    }
                }
            }

            return true
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun showNotificationSettingsOreo(key: String) {
        val intent = Intent()
        intent.setAction(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, context?.packageName)
        val type = if (key == NotificationType.TASK.soundKey)
            NotificationType.TASK
        else
            NotificationType.MESSAGE
        intent.putExtra(Settings.EXTRA_CHANNEL_ID, type.channelId)
        context?.startActivity(intent)
    }

    private fun showNotificationSettingsBelowOreo() {
        val intent = Intent()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        } else {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        }
        intent.putExtra("app_package", context?.packageName)
        intent.putExtra("app_uid", context?.applicationInfo?.uid)
        context?.startActivity(intent)
    }
}