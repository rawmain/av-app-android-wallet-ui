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

package eu.europa.ec.onboardingfeature.session

import android.graphics.Bitmap
import java.util.concurrent.ConcurrentHashMap

/**
 * Holds the passport face bitmap in memory for the duration of the passport onboarding flow.
 * Nothing is written to disk. On process death the bitmap is gone, which is the correct security
 * behaviour — re-capture is cheap and there is no reason to persist biometric material.
 */
class PassportOnboardingSession {

    private val bitmaps = ConcurrentHashMap<String, Bitmap>()

    fun put(sessionId: String, bitmap: Bitmap) {
        bitmaps[sessionId] = bitmap
    }

    fun get(sessionId: String): Bitmap? = bitmaps[sessionId]

    fun remove(sessionId: String) {
        bitmaps.remove(sessionId)?.recycle()
    }

    fun clear() {
        bitmaps.values.forEach { it.recycle() }
        bitmaps.clear()
    }
}
