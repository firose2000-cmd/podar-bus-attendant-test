package org.podar.busattendant

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Bearer token is encrypted at rest. Never store passwords. */
object SecureSession {
    private const val ALIAS = "podar_attendant_v16_session"
    private const val PREF = "v16_secure_session"
    private fun key(): SecretKey {
        val store=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS,null) as? SecretKey)?.let { return it }
        val gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore")
        gen.init(KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build())
        return gen.generateKey()
    }
    fun save(ctx:Context, token:String) {
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE,key())
        val blob=cipher.iv+cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        check(ctx.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit()
            .putString("token",Base64.encodeToString(blob,Base64.NO_WRAP)).commit())
    }
    fun read(ctx:Context):String? {
        val stored=ctx.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString("token",null)?:return null
        try {
            val bytes=Base64.decode(stored,Base64.NO_WRAP)
            val cipher=Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,bytes.copyOfRange(0,12)))
            return String(cipher.doFinal(bytes.copyOfRange(12,bytes.size)),Charsets.UTF_8)
        } catch(ex:Exception) { throw IllegalStateException("Saved login cannot be unlocked. Sign in again; offline attendance remains safe.",ex) }
    }
    fun clear(ctx:Context) { ctx.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().remove("token").commit() }
}
