package com.openmausbot.companion.audio

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * A call must outlive both the chat screen and the process leaving the
 * foreground. These wiring facts no runtime test in this module can
 * prove without instantiating the real Application (DataStore, the Keystore,
 * MediaPlayer), so they are pinned from the source, the way
 * `SessionLingerWiringTest` pins the linger install.
 */
class LiveCallWiringTest {
    @Test
    fun applicationOwnsManagerButNotOnStopTeardown() {
        val source = sourceFile("OpenMausApp.kt").readText()
        assertTrue(source.contains("liveCalls = LiveCallManager(this, session, appScope)"), "app-scoped, on the app scope")
        assertTrue(
            !source.contains("ProcessLifecycleOwner.get().lifecycle.addObserver(liveCalls)"),
            "Home or screen lock must not end the call",
        )
        assertTrue(source.contains("LiveCallForegroundState.active"), "background stream stays connected")
    }

    @Test
    fun `the Activity hands the same manager to every screen`() {
        val source = sourceFile("MainActivity.kt").readText()
        assertTrue(source.contains("liveCalls = app.liveCalls"), "screens must reach the one app-scoped manager")
    }

    private fun sourceFile(name: String): File {
        var directory: File? = File(".").absoluteFile
        while (directory != null) {
            for (prefix in listOf("", "app/")) {
                val file = File(directory, prefix + "src/main/kotlin/com/openmausbot/companion/$name")
                if (file.isFile) return file
            }
            directory = directory.parentFile
        }
        error("could not find $name from ${File(".").absolutePath}")
    }
}
