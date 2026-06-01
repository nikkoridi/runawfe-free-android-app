package ru.runa.wfe.notification

import android.Manifest
import android.os.Build
import androidx.test.rule.GrantPermissionRule
import ru.runa.wfe.restapi.model.WfeTask
import java.time.OffsetDateTime

object TestHelpers {
    fun generateNewTasks(new: Int, size: Int): List<WfeTask> {
        val expectedNewTasks =
            List(new) { WfeTask(name = "new", assignDate = OffsetDateTime.now(), firstOpen = true) }
        val tasks = (
                List(size - new) {
                    WfeTask(
                        name = "test",
                        assignDate = OffsetDateTime.now().minusDays(7),
                        firstOpen = false
                    )
                } + expectedNewTasks
                ).shuffled()
        return tasks
    }

    fun grantPostNotificationPermission(): GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            GrantPermissionRule.grant()
        }

}