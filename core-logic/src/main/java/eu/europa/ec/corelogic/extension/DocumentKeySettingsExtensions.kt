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

package eu.europa.ec.corelogic.extension

import eu.europa.ec.authenticationlogic.controller.storage.BiometryStorageController
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings.CredentialPolicy
import eu.europa.ec.eudi.wallet.document.DocumentExtensions.getDefaultCreateDocumentSettings
import eu.europa.ec.eudi.wallet.issue.openid4vci.Offer
import org.multipaz.securearea.UserAuthenticationType
import kotlin.time.Duration.Companion.seconds

/**
 * Builds [CreateDocumentSettings] for a newly issued document, deriving the key-creation
 * policy from the user's current biometric choice. StrongBox use follows
 * [EudiWallet.config.useStrongBoxForKeys], which is already auto-downgraded to `false` at
 * wallet construction on devices without StrongBox (e.g. emulators). User authentication
 * (LSKF + biometric, 30s timeout) is required only if the user opted in during onboarding.
 * This is evaluated per-issuance rather than at wallet construction so the setting reflects
 * the latest user choice.
 */
fun EudiWallet.createDocumentSettingsForUser(
    offeredDocument: Offer.OfferedDocument,
    credentialPolicy: CredentialPolicy,
    numberOfCredentials: Int,
    biometryStorageController: BiometryStorageController,
): CreateDocumentSettings {
    val userAuthRequired = biometryStorageController.getUseBiometricsAuth()
    return getDefaultCreateDocumentSettings(
        offeredDocument = offeredDocument,
        credentialPolicy = credentialPolicy,
        numberOfCredentials = numberOfCredentials,
        configure = {
            setUseStrongBox(config.useStrongBoxForKeys)
            setUserAuthenticationRequired(
                required = userAuthRequired,
                timeout = 30.seconds,
                userAuthenticationTypes = setOf(
                    UserAuthenticationType.LSKF,
                    UserAuthenticationType.BIOMETRIC
                )
            )
        }
    )
}
