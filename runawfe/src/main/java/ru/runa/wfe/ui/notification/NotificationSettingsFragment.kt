package ru.runa.wfe.ui.notification

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.launch
import ru.runa.wfe.MainActivity
import ru.runa.wfe.data.PreferencesManager
import ru.runa.wfe.R
import ru.runa.wfe.notification.NotificationHelpers
import ru.runa.wfe.notification.NotificationHelpers.NotificationType
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

    private fun pollingIntervalSummary(number: Int): String {
        return "$number ${resources.getQuantityString(R.plurals.minutes, number)}"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.notification_settings, rootKey)
        preferencesManager = PreferencesManager(requireContext())

        // Set custom preference and it's summary
        val pollingInterval: DurationPreference? = findPreference("pollingInterval")
        pollingInterval?.summary = pollingInterval?.duration?.let { pollingIntervalSummary(it) }

        lifecycleScope.launch {
            if (!preferencesManager.hasKey(PreferencesManager.POLLING_INTERVAL)) {
                preferencesManager.setKey(
                    PreferencesManager.POLLING_INTERVAL,
                    DurationPreference.DEFAULT
                )
            }
        }

        pollingInterval?.setOnPreferenceChangeListener { _, newValue ->
                val newInterval = newValue.toString().toIntOrNull()
                val currentPollingInterval = preferencesManager.getValue(PreferencesManager.POLLING_INTERVAL, 3)
                newInterval?.let {
                    if (newInterval != currentPollingInterval) {
                        lifecycleScope.launch {
                            preferencesManager.setKey(
                                PreferencesManager.POLLING_INTERVAL,
                                newInterval
                            )
                        }
                        pollingInterval.summary = pollingIntervalSummary(newInterval)
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

    private fun runRingtonePicker(title: String) {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, title)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, soundUri)
        ringtonePickerLauncher.launch(intent)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun showPostNotificationPermissionRequest(type: NotificationType) {
        val activity: MainActivity? = (activity as? MainActivity)
        activity?.requestPermission(Manifest.permission.POST_NOTIFICATIONS) { isGranted ->
            if (isGranted) {
                activity.startNotifying()
                showNotificationSettingsOreo(type)
            }
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        val channelType = when (preference.key) {
            NotificationType.TASK.preferenceName -> NotificationType.TASK
            NotificationType.MESSAGE.preferenceName -> NotificationType.MESSAGE
            else -> {
                NotificationType.DEFAULT
            }
        }
        if (channelType != NotificationType.DEFAULT) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!NotificationHelpers.channelExists(channelType, requireContext())) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        showPostNotificationPermissionRequest(channelType)
                    } else {
                        val explainDialogBuilder = AlertDialog.Builder(requireContext())
                            .setTitle(R.string.channel_not_exist)
                            .setMessage(
                                permissionsConstantsMap["POST_NOTIFICATIONS"]?.explanation
                                    ?: R.string.permission_need
                            )
                            .setNeutralButton("OK", null)
                        explainDialogBuilder.create().show()
                    }
                } else {
                    showNotificationSettingsOreo(channelType)
                }
            } else {
                // To show settings screen on old versions
                // showNotificationSettingsBelowOreo()

                // Show custom ringtone picker on old versions
                when (channelType) {
                    NotificationType.TASK -> {
                        runRingtonePicker(preference.title.toString())
                        lastPickedSoundKey = channelType.preferenceName
                        return true
                    }

                    NotificationType.MESSAGE -> {
                        runRingtonePicker(preference.title.toString())
                        lastPickedSoundKey = channelType.preferenceName
                        return true
                    }

                    else -> {
                        return true
                    }
                }
            }

            return true
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun showNotificationSettingsOreo(type: NotificationType) {
        val intent = Intent()
        intent.setAction(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, context?.packageName)
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