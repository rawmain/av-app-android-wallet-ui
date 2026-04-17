/*
 * Copyright (c) 2025 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package eu.europa.ec.businesslogic.controller.device

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import eu.europa.ec.businesslogic.controller.log.LogController
import java.io.File
import java.util.concurrent.TimeUnit

enum class DeviceIntegrityLevel {
    TRUSTED,
    POTENTIALLY_COMPROMISED,
    COMPROMISED
}

data class DeviceIntegrityResult(
    val level: DeviceIntegrityLevel,
    val rootDetected: Boolean,
    val emulatorDetected: Boolean,
    val debugBuild: Boolean,
    val details: List<String>
)

interface DeviceIntegrityController {
    fun checkIntegrity(): DeviceIntegrityResult
    fun isDeviceTrusted(): Boolean
}

class DeviceIntegrityControllerImpl(
    private val context: Context,
    private val logController: LogController
) : DeviceIntegrityController {

    override fun checkIntegrity(): DeviceIntegrityResult {
        val isDebuggable = (context.applicationInfo.flags and
                android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

        if (isDebuggable) {
            return DeviceIntegrityResult(
                level = DeviceIntegrityLevel.TRUSTED,
                rootDetected = false,
                emulatorDetected = false,
                debugBuild = true,
                details = listOf("Debug build — integrity checks skipped")
            )
        }

        val details = mutableListOf<String>()

        val rootDetected = checkRootIndicators(details)
        val emulatorDetected = checkEmulatorIndicators(details)

        val level = when {
            rootDetected -> DeviceIntegrityLevel.COMPROMISED
            emulatorDetected -> DeviceIntegrityLevel.POTENTIALLY_COMPROMISED
            else -> DeviceIntegrityLevel.TRUSTED
        }

        return DeviceIntegrityResult(
            level = level,
            rootDetected = rootDetected,
            emulatorDetected = emulatorDetected,
            debugBuild = false,
            details = details
        )
    }

    override fun isDeviceTrusted(): Boolean {
        return checkIntegrity().level == DeviceIntegrityLevel.TRUSTED
    }

    private fun checkRootIndicators(details: MutableList<String>): Boolean {
        var detected = false

        val suPaths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/su",
            "/data/local/su",
            "/data/local/bin/su",
            "/data/local/xbin/su",
            "/system/app/Superuser.apk",
            "/system/app/SuperSU.apk"
        )
        for (path in suPaths) {
            try {
                if (File(path).exists()) {
                    details.add("su binary found: $path")
                    detected = true
                }
            } catch (_: Throwable) {
                // SecurityException on some devices
            }
        }

        val rootPackages = listOf(
            "com.topjohnwu.magisk",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            "com.noshufou.android.su",
            "com.yellowes.su"
        )
        val pm = context.packageManager
        for (pkg in rootPackages) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkg, 0)
                }
                details.add("Root management app installed: $pkg")
                detected = true
            } catch (_: Throwable) {
                // Not installed or not visible
            }
        }

        try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val completed = process.waitFor(3, TimeUnit.SECONDS)
            if (completed) {
                val result = process.inputStream.bufferedReader().use { it.readLine() }
                if (!result.isNullOrBlank()) {
                    details.add("su found via which: $result")
                    detected = true
                }
            }
            process.destroy()
        } catch (_: Throwable) {
            // Expected on non-rooted devices
        }

        if (Build.TAGS?.contains("test-keys") == true) {
            details.add("Device uses test-keys")
            detected = true
        }

        return detected
    }

    private fun checkEmulatorIndicators(details: MutableList<String>): Boolean {
        val indicators = mutableListOf<String>()

        if (Build.FINGERPRINT.contains("generic") || Build.FINGERPRINT.contains("vbox")) {
            indicators.add("Build fingerprint: ${Build.FINGERPRINT}")
        }
        if (Build.MODEL.contains("Emulator") || Build.MODEL.contains("Android SDK")) {
            indicators.add("Build model: ${Build.MODEL}")
        }
        if (Build.MANUFACTURER.contains("Genymotion")) {
            indicators.add("Manufacturer: ${Build.MANUFACTURER}")
        }
        if (Build.HARDWARE.contains("goldfish") || Build.HARDWARE.contains("ranchu")) {
            indicators.add("Hardware: ${Build.HARDWARE}")
        }
        if (Build.PRODUCT.contains("sdk") || Build.PRODUCT.contains("emulator")) {
            indicators.add("Product: ${Build.PRODUCT}")
        }

        if (indicators.isNotEmpty()) {
            details.addAll(indicators)
            return true
        }
        return false
    }
}
