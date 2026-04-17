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

package eu.europa.ec.businesslogic.controller.storage

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import eu.europa.ec.resourceslogic.provider.ResourceProvider

interface PrefsController {

    fun contains(key: String): Boolean
    fun clear(key: String)
    fun clear()
    fun setString(key: String, value: String)
    fun setLong(key: String, value: Long)
    fun setBool(key: String, value: Boolean)
    fun getString(key: String, defaultValue: String): String
    fun getLong(key: String, defaultValue: Long): Long
    fun getBool(key: String, defaultValue: Boolean): Boolean
    fun setInt(key: String, value: Int)
    fun getInt(key: String, defaultValue: Int): Int
}

class PrefsControllerImpl(
    private val resourceProvider: ResourceProvider
) : PrefsController {

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "eudi-wallet-encrypted",
            masterKeyAlias,
            resourceProvider.provideContext(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun getSharedPrefs(): SharedPreferences = encryptedPrefs

    override fun contains(key: String): Boolean {
        return getSharedPrefs().contains(key)
    }

    override fun clear(key: String) {
        getSharedPrefs().edit { remove(key) }
    }

    override fun clear() {
        getSharedPrefs().edit { clear() }
    }

    override fun setString(key: String, value: String) {
        getSharedPrefs().edit {
            putString(key, value)
        }
    }

    override fun setLong(key: String, value: Long) {
        getSharedPrefs().edit {
            putLong(key, value)
        }
    }

    override fun setBool(key: String, value: Boolean) {
        getSharedPrefs().edit {
            putBoolean(key, value)
        }
    }

    override fun getString(key: String, defaultValue: String): String {
        return getSharedPrefs().getString(key, null) ?: defaultValue
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return getSharedPrefs().getLong(key, defaultValue)
    }

    override fun getBool(key: String, defaultValue: Boolean): Boolean {
        return getSharedPrefs().getBoolean(key, defaultValue)
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return getSharedPrefs().getInt(key, defaultValue)
    }

    override fun setInt(key: String, value: Int) {
        getSharedPrefs().edit {
            putInt(key, value)
        }
    }
}

interface PrefKeys {
    fun getCryptoAlias(): String
    fun setCryptoAlias(value: String)
    fun getCryptoKeyVersion(): Int
    fun setCryptoKeyVersion(value: Int)
    fun getPreviousCryptoAlias(): String
    fun setPreviousCryptoAlias(value: String)
    fun clearPreviousCryptoAlias()
}

class PrefKeysImpl(
    private val prefsController: PrefsController
) : PrefKeys {

    override fun getCryptoAlias(): String {
        return prefsController.getString("CryptoAlias", "")
    }

    override fun setCryptoAlias(value: String) {
        prefsController.setString("CryptoAlias", value)
    }

    override fun getCryptoKeyVersion(): Int {
        return prefsController.getInt("CryptoKeyVersion", 1)
    }

    override fun setCryptoKeyVersion(value: Int) {
        prefsController.setInt("CryptoKeyVersion", value)
    }

    override fun getPreviousCryptoAlias(): String {
        return prefsController.getString("PreviousCryptoAlias", "")
    }

    override fun setPreviousCryptoAlias(value: String) {
        prefsController.setString("PreviousCryptoAlias", value)
    }

    override fun clearPreviousCryptoAlias() {
        prefsController.clear("PreviousCryptoAlias")
    }
}
