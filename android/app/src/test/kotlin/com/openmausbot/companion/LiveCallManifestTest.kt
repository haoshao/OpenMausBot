package com.openmausbot.companion

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertTrue
import org.w3c.dom.Element

/**
 * Live calls need two permissions no compiler checks: RECORD_AUDIO (already
 * there for dictation) and MODIFY_AUDIO_SETTINGS, without which
 * `AudioManager.mode` and the speaker/earpiece route silently do nothing.
 * Pinned from the manifest the way `PairingLinkManifestTest` pins its attributes.
 */
class LiveCallManifestTest {
    private val android = "http://schemas.android.com/apk/res/android"

    @Test
    fun declaresTheAudioPermissionsACallNeeds() {
        val names = permissions()
        assertTrue("android.permission.RECORD_AUDIO" in names)
        assertTrue("android.permission.MODIFY_AUDIO_SETTINGS" in names)
    }

    @Test
    fun declaresAndroidMicrophoneForegroundService() {
        assertTrue("android.permission.FOREGROUND_SERVICE" in permissions())
        assertTrue("android.permission.FOREGROUND_SERVICE_MICROPHONE" in permissions())
        val document = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(locateManifest())
        val services = document.documentElement.getElementsByTagName("service")
        assertTrue((0 until services.length).any { index ->
            val service = services.item(index) as Element
            service.getAttributeNS(android, "name") == ".audio.LiveCallForegroundService" &&
                service.getAttributeNS(android, "foregroundServiceType") == "microphone" &&
                service.getAttributeNS(android, "exported") == "false" &&
                service.getAttributeNS(android, "stopWithTask") == "false"
        }, "keep microphone foreground-serviced when Home or lock screen is pressed")
    }

    private fun permissions(): Set<String> {
        val document = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(locateManifest())
        val nodes = document.documentElement.getElementsByTagName("uses-permission")
        return (0 until nodes.length).map { (nodes.item(it) as Element).getAttributeNS(android, "name") }.toSet()
    }

    private fun locateManifest(): File {
        var directory: File? = File(".").absoluteFile
        while (directory != null) {
            for (candidate in listOf("src/main/AndroidManifest.xml", "app/src/main/AndroidManifest.xml")) {
                val file = File(directory, candidate)
                if (file.isFile) return file
            }
            directory = directory.parentFile
        }
        error("could not find AndroidManifest.xml from ${File(".").absolutePath}")
    }
}
