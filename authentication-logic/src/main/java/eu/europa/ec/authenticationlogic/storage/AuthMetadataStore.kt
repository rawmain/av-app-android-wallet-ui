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

package eu.europa.ec.authenticationlogic.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Base64
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.businesslogic.controller.storage.PrefsController
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// Decision: store the encrypted blob inside SecurePrefsStore (double-wrapping). This provides
// defence-in-depth at no design cost since SecurePrefsStore is already wired.
class AuthMetadataStore(
    private val prefsController: PrefsController,
    private val logController: LogController,
) {
    companion object {
        private const val TAG = "AuthMetadataStore"
        const val META_KEY_ALIAS = "av_pin_meta_v1"
        private const val BLOB_KEY = "auth_metadata_blob"
        private const val COUNTER_KEY = "auth_write_counter_mirror"
        private const val GCM_IV_SIZE = 12
        private const val GCM_TAG_BITS = 128
        private const val STORE_TYPE = "AndroidKeyStore"
    }

    private val mutex = Mutex()

    // In-memory cache of the last verified metadata. Avoids repeated StrongBox decrypts within
    // the same process lifetime. Invalidated on every write and wipe.
    private var cache: AuthMetadata? = null

    private val metaKey: SecretKey by lazy { getOrCreateMetaKey() }

    private fun getOrCreateMetaKey(): SecretKey {
        val ks = KeyStore.getInstance(STORE_TYPE).also { it.load(null) }
        if (ks.containsAlias(META_KEY_ALIAS)) {
            return ks.getKey(META_KEY_ALIAS, null) as SecretKey
        }
        return generateMetaKey(tryStrongBox = true)
    }

    private fun generateMetaKey(tryStrongBox: Boolean): SecretKey {
        val builder = KeyGenParameterSpec.Builder(
            META_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false)
            .setIsStrongBoxBacked(tryStrongBox)
        return try {
            val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, STORE_TYPE)
            kg.init(builder.build())
            kg.generateKey()
            val ks = KeyStore.getInstance(STORE_TYPE).also { it.load(null) }
            val tier = if (tryStrongBox) "StrongBox" else "TEE"
            logController.d(TAG) { "Created meta key in $tier" }
            ks.getKey(META_KEY_ALIAS, null) as SecretKey
        } catch (e: StrongBoxUnavailableException) {
            if (tryStrongBox) generateMetaKey(tryStrongBox = false) else throw e
        }
    }

    private fun encrypt(plaintext: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, metaKey)
        val combined = cipher.iv + cipher.doFinal(plaintext)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): ByteArray? {
        return try {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            if (combined.size <= GCM_IV_SIZE) return null
            val iv = combined.copyOfRange(0, GCM_IV_SIZE)
            val ciphertext = combined.copyOfRange(GCM_IV_SIZE, combined.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, metaKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.doFinal(ciphertext)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun read(): AuthMetadata? = mutex.withLock {
        cache?.let { return@withLock it }

        val encoded = prefsController.getString(BLOB_KEY, "")
        if (encoded.isEmpty()) return@withLock null
        val plaintext = decrypt(encoded) ?: return@withLock null
        val meta = try {
            AuthMetadataCodec.decode(plaintext)
        } catch (_: AuthMetadataCorruptException) {
            return@withLock null
        }
        // Rollback check: only required on cold read; once cached the write counter is authoritative.
        val mirrorCounter = readMirrorCounter()
        if (meta.writeCounter != mirrorCounter) {
            logController.d(TAG) { "Rollback detected — wiping auth metadata" }
            wipeInternal()
            return@withLock null
        }
        cache = meta
        meta
    }

    suspend fun write(meta: AuthMetadata) = mutex.withLock {
        val next = meta.copy(writeCounter = meta.writeCounter + 1)
        val plaintext = AuthMetadataCodec.encode(next)
        val encoded = encrypt(plaintext)
        prefsController.setString(BLOB_KEY, encoded)
        writeMirrorCounter(next.writeCounter)
        cache = next
    }

    suspend fun wipe() = mutex.withLock {
        wipeInternal()
    }

    private fun wipeInternal() {
        prefsController.clear(BLOB_KEY)
        prefsController.clear(COUNTER_KEY)
        cache = null
    }

    private fun readMirrorCounter(): Long {
        val encoded = prefsController.getString(COUNTER_KEY, "")
        if (encoded.isEmpty()) return 0L
        val raw = decrypt(encoded) ?: return 0L
        if (raw.size < 8) return 0L
        return ByteBuffer.wrap(raw).long
    }

    private fun writeMirrorCounter(counter: Long) {
        val bytes = ByteBuffer.allocate(8).putLong(counter).array()
        prefsController.setString(COUNTER_KEY, encrypt(bytes))
    }
}
