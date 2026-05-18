/*
 * Copyright (c) 2026 European Commission
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

package eu.europa.ec.authenticationlogic.controller.authentication

import android.os.Build
import androidx.biometric.BiometricManager
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Test

class TestDeviceAuthenticationController {

    @Test
    fun `Given API 30 or above, When resolving prompt configuration, Then biometrics and device credential are both allowed`() {
        val result = resolvePromptConfiguration(
            sdkInt = Build.VERSION_CODES.R,
            hasCryptoObject = true,
            cancelButtonText = "Cancel"
        )

        assertEquals(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            result.allowedAuthenticators
        )
        assertFalse(result.isDeviceCredentialAllowed)
        assertNull(result.negativeButtonText)
    }

    @Test
    fun `Given API 29 with crypto object, When resolving prompt configuration, Then strong biometrics and cancel button are used`() {
        val result = resolvePromptConfiguration(
            sdkInt = Build.VERSION_CODES.Q,
            hasCryptoObject = true,
            cancelButtonText = "Cancel"
        )

        assertEquals(BiometricManager.Authenticators.BIOMETRIC_STRONG, result.allowedAuthenticators)
        assertFalse(result.isDeviceCredentialAllowed)
        assertEquals("Cancel", result.negativeButtonText)
    }

    @Test
    fun `Given API 29 without crypto object, When resolving prompt configuration, Then device credential fallback is enabled`() {
        val result = resolvePromptConfiguration(
            sdkInt = Build.VERSION_CODES.Q,
            hasCryptoObject = false,
            cancelButtonText = "Cancel"
        )

        assertNull(result.allowedAuthenticators)
        assertTrue(result.isDeviceCredentialAllowed)
        assertNull(result.negativeButtonText)
    }
}
