/*
 * Copyright (c) 2023 European Commission
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

package eu.europa.ec.authenticationlogic.storage

import eu.europa.ec.authenticationlogic.provider.PinStorageProvider
import eu.europa.ec.businesslogic.controller.storage.PrefsController

class PrefsPinStorageProvider(
    private val prefsController: PrefsController
) : PinStorageProvider {

    companion object {
        private const val KEY_DEVICE_PIN = "DevicePin"
        private const val KEY_FAILED_ATTEMPTS = "PinFailedAttempts"
        private const val KEY_LOCKOUT_UNTIL = "PinLockoutUntil"
    }

    override fun retrievePin(): String {
        return prefsController.getString(KEY_DEVICE_PIN, "")
    }

    override fun setPin(pin: String) {
        prefsController.setString(KEY_DEVICE_PIN, pin)
        resetFailedAttempts()
    }

    override fun isPinValid(pin: String): Boolean = retrievePin() == pin

    override fun getFailedAttempts(): Int {
        return prefsController.getInt(KEY_FAILED_ATTEMPTS, 0)
    }

    override fun incrementFailedAttempts(): Int {
        val currentAttempts = getFailedAttempts() + 1
        prefsController.setInt(KEY_FAILED_ATTEMPTS, currentAttempts)
        return currentAttempts
    }

    override fun resetFailedAttempts() {
        prefsController.setInt(KEY_FAILED_ATTEMPTS, 0)
        prefsController.setLong(KEY_LOCKOUT_UNTIL, 0L)
    }

    override fun setLockoutUntil(timestampMillis: Long) {
        prefsController.setLong(KEY_LOCKOUT_UNTIL, timestampMillis)
    }

    override fun getLockoutUntil(): Long {
        return prefsController.getLong(KEY_LOCKOUT_UNTIL, 0L)
    }

    override fun isCurrentlyLockedOut(): Boolean {
        val lockoutUntil = getLockoutUntil()
        return lockoutUntil > 0L && System.currentTimeMillis() < lockoutUntil
    }
}