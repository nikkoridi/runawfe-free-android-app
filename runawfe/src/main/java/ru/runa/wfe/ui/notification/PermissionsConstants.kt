package ru.runa.wfe.ui.notification

import android.provider.Settings
import ru.runa.wfe.R

val permissionsConstantsMap: Map<String, PermissionsConstants> = mapOf(
    "POST_NOTIFICATIONS" to PermissionsConstants(
        R.string.permission_notification_need,
        Settings.ACTION_APP_NOTIFICATION_SETTINGS
    )
)

data class PermissionsConstants(val explanation: Int, val settingsPage: String)