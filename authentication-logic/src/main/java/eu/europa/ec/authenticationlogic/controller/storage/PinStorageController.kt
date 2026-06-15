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

package eu.europa.ec.authenticationlogic.controller.storage

import eu.europa.ec.authenticationlogic.config.StorageConfig
import eu.europa.ec.businesslogic.provider.ElapsedRealtimeClock
import kotlinx.coroutines.delay

sealed class PinValidationResult {
    object Success : PinValidationResult()
    data class Failed(val attemptsRemaining: Int) : PinValidationResult()
    data class LockedOut(val lockoutEndTimeMillis: Long, val attemptsUsed: Int) :
        PinValidationResult()
}

interface PinStorageController {
    fun hasPin(): Boolean
    fun setPin(pin: String)
    suspend fun isPinValid(pin: String): PinValidationResult
}

class PinStorageControllerImpl(
    private val storageConfig: StorageConfig,
    private val clock: ElapsedRealtimeClock,
) : PinStorageController {

    companion object {
        private const val MAX_ATTEMPTS = 4
        private const val BASE_LOCKOUT_DURATION_MS = 60 * 1000L
        private const val BASE_ATTEMPT_DELAY_MS = 1_000L
    }

    private val pinStorageProvider = storageConfig.pinStorageProvider

    override fun hasPin(): Boolean = pinStorageProvider.hasPin()

    override fun setPin(pin: String) {
        pinStorageProvider.setPin(pin)
    }

    override suspend fun isPinValid(pin: String): PinValidationResult {
        val failedAttempts = pinStorageProvider.getFailedAttempts()
        val lockoutUntil = pinStorageProvider.getLockoutUntil()
        if (lockoutUntil > 0L && clock.now() < lockoutUntil) {
            return PinValidationResult.LockedOut(
                lockoutEndTimeMillis = lockoutUntil,
                attemptsUsed = failedAttempts
            )
        }

        val enforceMs = BASE_ATTEMPT_DELAY_MS * (1 + failedAttempts)
        val start = clock.now()
        val valid = pinStorageProvider.isPinValid(pin)

        return if (valid) {
            if (failedAttempts > 0) pinStorageProvider.resetFailedAttempts()
            PinValidationResult.Success
        } else {
            val elapsed = clock.now() - start
            val remaining = enforceMs - elapsed
            if (remaining > 0) {
                delay(remaining)
            }
            val newAttemptCount = pinStorageProvider.incrementFailedAttempts()

            if (newAttemptCount >= MAX_ATTEMPTS) {
                val lockoutDuration = calculateLockoutDuration(newAttemptCount)
                // Intentional: the deadline is anchored to elapsedRealtime() via the storage
                // provider so advancing the system clock cannot bypass a lockout. The provider
                // also binds the deadline to BOOT_COUNT — on reboot the deadline is re-anchored
                // to now + duration so attackers gain nothing from rebooting.
                pinStorageProvider.setLockoutForDuration(lockoutDuration)

                PinValidationResult.LockedOut(
                    lockoutEndTimeMillis = pinStorageProvider.getLockoutUntil(),
                    attemptsUsed = newAttemptCount
                )
            } else {
                PinValidationResult.Failed(
                    attemptsRemaining = MAX_ATTEMPTS - newAttemptCount
                )
            }
        }
    }

    private fun calculateLockoutDuration(attemptCount: Int): Long {
        // Progressive lockout times: 3=none, 4=1min, 5=5min, 6=15min, 7=1hr, 8=3hr, 9=8hr
        val multiplier = when (attemptCount) {
            4 -> 1
            5 -> 5
            6 -> 15
            7 -> 60
            8 -> 180
            9 -> 480
            else -> if (attemptCount > 9) 480 else 0
        }
        return BASE_LOCKOUT_DURATION_MS * multiplier
    }
}
