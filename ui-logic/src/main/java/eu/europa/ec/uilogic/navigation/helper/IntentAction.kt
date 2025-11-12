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

package eu.europa.ec.uilogic.navigation.helper

import android.content.Intent

data class IntentAction(val intent: Intent)

private const val ACTION_GET_CREDENTIAL =
    "androidx.credentials.registry.provider.action.GET_CREDENTIAL"
private const val ACTION_GET_CREDENTIALS = "androidx.identitycredentials.action.GET_CREDENTIALS"

/**
 * Temporary storage for DCAPI intent to make it accessible across the app lifecycle
 * This is needed because the intent needs to survive context changes between caching and retrieval
 */
object DcApiIntentHolder {
    private var cachedIntent: Intent? = null

    fun cacheIntent(intent: Intent?) {
        cachedIntent = intent
    }

    fun retrieveIntent(): Intent? {
        val intent = cachedIntent
        cachedIntent = null
        return intent
    }
}

fun isDCAPIIntent(intent: Intent?): Boolean {
    return intent?.action == ACTION_GET_CREDENTIAL || intent?.action == ACTION_GET_CREDENTIALS
}
