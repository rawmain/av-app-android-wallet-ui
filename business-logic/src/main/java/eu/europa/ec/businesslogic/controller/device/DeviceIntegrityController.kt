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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

private val ROOT_PACKAGES = listOf(
    "com.topjohnwu.magisk",
    "eu.chainfire.supersu",
    "com.koushikdutta.superuser",
    "com.thirdparty.superuser",
    "com.noshufou.android.su",
    "com.yellowes.su",
    "com.kingroot.kinguser",
    "com.kingo.root",
    "com.smedialink.oneclickroot",
    "com.zhiqupk.root.global",
    "com.alephzain.framaroot",
    "com.koushikdutta.rommanager",
    "com.dimonvideo.luckypatcher",
    "com.chelpus.lackypatch",
)

private val HOOK_PACKAGES = listOf(
    "de.robv.android.xposed.installer",
    "org.lsposed.manager",
    "io.github.lsposed.manager",
    "com.saurik.substrate",
    "me.weishu.exp",
)

private val SU_PATHS = listOf(
    "/system/bin/su",
    "/system/xbin/su",
    "/sbin/su",
    "/system/su",
    "/data/local/su",
    "/data/local/bin/su",
    "/data/local/xbin/su",
    "/system/app/Superuser.apk",
    "/system/app/SuperSU.apk",
)

private val PROC_MAPS_HOOKS = listOf(
    "frida",
    "xposed",
    "substrate",
    "lsposed",
)

enum class DeviceIntegrityLevel {
    TRUSTED,
    POTENTIALLY_COMPROMISED,
    COMPROMISED
}

data class DeviceIntegrityResult(
    val level: DeviceIntegrityLevel,
    val rootDetected: Boolean,
    val emulatorDetected: Boolean,
    val hookingFrameworkDetected: Boolean,
    val debugBuild: Boolean,
    val details: List<String>
)

interface DeviceIntegrityController {
    suspend fun checkIntegrity(): DeviceIntegrityResult
}

class DeviceIntegrityControllerImpl(
    private val context: Context,
    private val logController: LogController
) : DeviceIntegrityController {

    override suspend fun checkIntegrity(): DeviceIntegrityResult = withContext(Dispatchers.IO) {
        val isDebuggable = (context.applicationInfo.flags and
                android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

        // Intentional: debug builds skip all integrity checks to allow development and testing
        // on emulators and non-production devices. This is acceptable because debug APKs are
        // never distributed to end users.
        if (isDebuggable) {
            return@withContext DeviceIntegrityResult(
                level = DeviceIntegrityLevel.TRUSTED,
                rootDetected = false,
                emulatorDetected = false,
                hookingFrameworkDetected = false,
                debugBuild = true,
                details = listOf("Debug build — integrity checks skipped")
            )
        }

        val details = mutableListOf<String>()

        val rootDetected = checkRootIndicators(details)
        val emulatorDetected = checkEmulatorIndicators(details)
        val hookDetected = checkHookingFrameworks(details)

        val level = when {
            rootDetected || hookDetected -> DeviceIntegrityLevel.COMPROMISED
            emulatorDetected -> DeviceIntegrityLevel.POTENTIALLY_COMPROMISED
            else -> DeviceIntegrityLevel.TRUSTED
        }

        DeviceIntegrityResult(
            level = level,
            rootDetected = rootDetected,
            emulatorDetected = emulatorDetected,
            hookingFrameworkDetected = hookDetected,
            debugBuild = false,
            details = details
        )
    }

    private fun checkRootIndicators(details: MutableList<String>): Boolean {
        for (path in SU_PATHS) {
            try {
                if (File(path).exists()) {
                    details.add("su binary found: $path")
                    return true
                }
            } catch (_: Throwable) {
                // SecurityException on some devices
            }
        }

        val pm = context.packageManager
        for (pkg in ROOT_PACKAGES) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkg, 0)
                }
                details.add("Root management app installed: $pkg")
                return true
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
                    process.destroy()
                    return true
                }
            }
            process.destroy()
        } catch (_: Throwable) {
            // Expected on non-rooted devices
        }

        if (Build.TAGS?.contains("test-keys") == true) {
            details.add("Device uses test-keys")
            return true
        }

        // Writable /system partition is a strong root indicator
        try {
            if (File("/system").canWrite()) {
                details.add("/system partition is writable")
                return true
            }
        } catch (_: Throwable) {}

        return false
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
        // ro.kernel.qemu=1 is set on QEMU-based emulators
        val qemu = readSystemProperty("ro.kernel.qemu")
        if (qemu == "1") {
            indicators.add("ro.kernel.qemu=1")
        }

        if (indicators.isNotEmpty()) {
            details.addAll(indicators)
            return true
        }
        return false
    }

    private fun checkHookingFrameworks(details: MutableList<String>): Boolean {
        // Installed Xposed / LSPosed / Substrate manager packages
        val pm = context.packageManager
        for (pkg in HOOK_PACKAGES) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkg, 0)
                }
                details.add("Hooking framework installed: $pkg")
                return true
            } catch (_: Throwable) {}
        }

        // XposedBridge is loaded into every hooked process via the class loader
        try {
            Class.forName("de.robv.android.xposed.XposedBridge")
            details.add("XposedBridge class found in class loader")
            return true
        } catch (_: Throwable) {}

        // /proc/self/maps reveals injected native libraries (Frida gadget, Xposed, Substrate)
        try {
            val found = File("/proc/self/maps").useLines { lines ->
                lines.any { line ->
                    val lower = line.lowercase()
                    val marker = PROC_MAPS_HOOKS.firstOrNull { lower.contains(it) }
                    if (marker != null) {
                        details.add("Hooking library in process memory: $marker")
                        true
                    } else false
                }
            }
            if (found) return true
        } catch (_: Throwable) {}

        // Frida default server port open on localhost
        if (isFridaPortOpen()) {
            details.add("Frida default server port (27042) open")
            return true
        }

        return false
    }

    // Caller already runs on Dispatchers.IO via checkIntegrity()
    private fun isFridaPortOpen(): Boolean {
        return try {
            val socket = java.net.Socket()
            socket.connect(
                java.net.InetSocketAddress("127.0.0.1", 27042),
                300
            )
            socket.close()
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun readSystemProperty(name: String): String? {
        return try {
            @Suppress("PrivateApi")
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java)
            method.invoke(null, name) as? String
        } catch (_: Throwable) {
            null
        }
    }
}
