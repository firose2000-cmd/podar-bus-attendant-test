package org.podar.busattendant

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts a random SQLCipher passphrase using a non-exportable Android Keystore key.
 * Never silently regenerates a key when encrypted data already exists.
 */
object EncryptedDbKey {
    private const val ALIAS = "podar_bus_attendant_v15_database"
    private const val PREF = "encrypted_database_key_v15"
    private const val WRAPPED = "wrapped_passphrase"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build())
        return generator.generateKey()
    }
    fun passphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val saved = prefs.getString(WRAPPED, null)
        if (saved != null) {
            try {
                val bytes = Base64.decode(saved, Base64.NO_WRAP)
                require(bytes.size > 12) { "Invalid encrypted database key" }
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
                return cipher.doFinal(bytes.copyOfRange(12, bytes.size))
            } catch (ex: Exception) {
                throw IllegalStateException("Cannot unlock saved offline attendance. Do not clear app data or reinstall; contact administrator.", ex)
            }
        }
        // Never generate a fresh passphrase if an encrypted database already exists.
        // Otherwise a lost preference would strand the user's unsynchronized records.
        val encryptedDb = context.getDatabasePath("attendant_v15_encrypted.db")
        if (encryptedDb.exists() || context.getDatabasePath("attendant_v15_encrypted.db-wal").exists()) {
            throw IllegalStateException("Encrypted offline attendance exists but its key is missing. Do not clear app data or reinstall; contact administrator.")
        }
        // No automatic migration from V1.4's plaintext DB: this is a new test database.
        val oldDb = context.getDatabasePath("attendant_v14.db")
        if (oldDb.exists()) throw IllegalStateException("Older unencrypted test database exists. Export/reconcile its pending attendance before migration.")
        val pass = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val blob = cipher.iv + cipher.doFinal(pass)
        check(prefs.edit().putString(WRAPPED, Base64.encodeToString(blob, Base64.NO_WRAP)).commit()) {
            "Unable to persist encryption key; refusing to create offline database"
        }
        return pass
    }
}
