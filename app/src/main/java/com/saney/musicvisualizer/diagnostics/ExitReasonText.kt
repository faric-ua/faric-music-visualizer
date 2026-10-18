package com.saney.musicvisualizer.diagnostics

/**
 * Plain-Ukrainian meaning of Android ApplicationExitInfo reasons (API 30+).
 *
 * Kept free of android.* so it runs in JVM unit tests. Integer values are the
 * public, stable ApplicationExitInfo.REASON_* constants.
 */
object ExitReasonText {
    const val REASON_UNKNOWN = 0
    const val REASON_EXIT_SELF = 1
    const val REASON_SIGNALED = 2
    const val REASON_LOW_MEMORY = 3
    const val REASON_CRASH = 4
    const val REASON_CRASH_NATIVE = 5
    const val REASON_ANR = 6
    const val REASON_INITIALIZATION_FAILURE = 7
    const val REASON_PERMISSION_CHANGE = 8
    const val REASON_EXCESSIVE_RESOURCE_USAGE = 9
    const val REASON_USER_REQUESTED = 10
    const val REASON_USER_STOPPED = 11
    const val REASON_DEPENDENCY_DIED = 12
    const val REASON_OTHER = 13
    const val REASON_FREEZER = 14
    const val REASON_PACKAGE_STATE_CHANGE = 15
    const val REASON_PACKAGE_UPDATED = 16

    fun describe(reason: Int): String =
        when (reason) {
            REASON_UNKNOWN -> "невідома причина (Android не сказав)"
            REASON_EXIT_SELF -> "застосунок завершився сам"
            REASON_SIGNALED -> "процес зупинила система (сигнал)"
            REASON_LOW_MEMORY -> "Android закрив через брак пам'яті"
            REASON_CRASH -> "падіння — помилка в коді (Kotlin / Java)"
            REASON_CRASH_NATIVE -> "падіння в C++ (projectM, відеокарта або кодек)"
            REASON_ANR -> "застосунок завис і не відповідав"
            REASON_INITIALIZATION_FAILURE -> "не зміг запуститись"
            REASON_PERMISSION_CHANGE -> "змінено дозволи застосунку"
            REASON_EXCESSIVE_RESOURCE_USAGE -> "Android закрив за надмірне навантаження (процесор / батарея)"
            REASON_USER_REQUESTED -> "закрито вручну (зупинка або прибрано з диспетчера задач)"
            REASON_USER_STOPPED -> "зупинено в налаштуваннях Android"
            REASON_DEPENDENCY_DIED -> "впала служба, від якої залежить застосунок"
            REASON_OTHER -> "інша причина системи"
            REASON_FREEZER -> "заморожено системою у фоні"
            REASON_PACKAGE_STATE_CHANGE -> "змінився стан застосунку (вимкнено / оновлено)"
            REASON_PACKAGE_UPDATED -> "встановлено нову версію APK"
            else -> "невідома причина (код $reason)"
        }

    /**
     * Exits worth showing to the owner on the next start. Normal ones (new APK
     * installed, closed by hand, the app finishing itself) are not reported unless
     * an export was running at that moment — the caller decides that.
     */
    fun isNotable(reason: Int): Boolean =
        when (reason) {
            REASON_EXIT_SELF,
            REASON_PERMISSION_CHANGE,
            REASON_USER_REQUESTED,
            REASON_USER_STOPPED,
            REASON_PACKAGE_STATE_CHANGE,
            REASON_PACKAGE_UPDATED,
            -> false
            else -> true
        }

    /** RunningAppProcessInfo.IMPORTANCE_* → where the app was when it died. */
    fun describeImportance(importance: Int): String =
        when {
            importance <= 0 -> "невідомо"
            importance <= 100 -> "на екрані"
            importance <= 125 -> "фонова служба"
            importance <= 230 -> "видно частково / за іншим вікном"
            importance <= 300 -> "у фоні (служба)"
            else -> "у фоні"
        }
}
