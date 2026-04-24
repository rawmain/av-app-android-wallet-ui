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

package eu.europa.ec.storagelogic.di

import android.content.Context
import android.os.Build
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import eu.europa.ec.storagelogic.dao.BookmarkDao
import eu.europa.ec.storagelogic.dao.RevokedDocumentDao
import eu.europa.ec.storagelogic.dao.TransactionLogDao
import eu.europa.ec.storagelogic.service.DatabaseService
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import java.security.SecureRandom

@Module
@ComponentScan("eu.europa.ec.storagelogic")
class LogicStorageModule

@Single
fun provideAppDatabase(context: Context): DatabaseService {
    System.loadLibrary("sqlcipher")
    val passphrase = getOrCreateDbPassphrase(context)
    val factory = SupportOpenHelperFactory(passphrase)
    return Room.databaseBuilder(
        context,
        DatabaseService::class.java,
        "eudi.app.wallet.storage"
    )
        .openHelperFactory(factory)
        // Intentional: this is a template repository with no shipped user data to preserve.
        // Teams forking for production must replace this with explicit Migration objects.
        .fallbackToDestructiveMigration(true)
        .build()
}

private const val DB_KEY_PREFS = "db-key-prefs"
private const val DB_PASSPHRASE_KEY = "db_passphrase"

private fun getOrCreateDbPassphrase(context: Context): ByteArray {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .setRequestStrongBoxBacked(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        .setUserAuthenticationRequired(false) // Passphrase key does not gate on biometrics; the DB cipher provides at-rest protection
        .build()
    val prefs = EncryptedSharedPreferences.create(
        context,
        DB_KEY_PREFS,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    val hex = prefs.getString(DB_PASSPHRASE_KEY, null) ?: run {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        val generated = bytes.joinToString("") { "%02x".format(it) }
        prefs.edit().putString(DB_PASSPHRASE_KEY, generated).apply()
        generated
    }
    return hex.toByteArray(Charsets.UTF_8)
}

@Single
fun provideBookmarkDao(service: DatabaseService): BookmarkDao = service.bookmarkDao()

@Single
fun provideRevokedDocumentDao(service: DatabaseService): RevokedDocumentDao =
    service.revokedDocumentDao()

@Single
fun provideTransactionLogDao(service: DatabaseService): TransactionLogDao =
    service.transactionLogDao()
