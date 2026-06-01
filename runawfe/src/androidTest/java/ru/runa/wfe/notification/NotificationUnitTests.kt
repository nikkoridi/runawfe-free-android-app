package ru.runa.wfe.notification

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses


@RunWith(Suite::class)
@SuiteClasses(
    NotificationServiceTest::class,
    NotificationHelpersTest::class,
    NotificationLogicTest::class,
    NotificationLogicApiDataTest::class,
    NotificationLogicMessagesContentTest::class
)
class NotificationUnitTests {
}