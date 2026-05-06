package ru.runa.wfe.notification

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

}