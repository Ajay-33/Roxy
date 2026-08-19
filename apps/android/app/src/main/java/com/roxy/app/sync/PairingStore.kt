package com.roxy.app.sync

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

class PairingStore(context: Context) {
    private val preferences = context.getSharedPreferences("pairing", Context.MODE_PRIVATE)
    private val alias = "roxy_device_credential_v1"

    fun save(endpoint: String, credential: String) {
        require(endpoint.startsWith("http://") || endpoint.startsWith("https://"))
        require(credential.length >= 32)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        preferences.edit().putString("endpoint", endpoint.removeSuffix("/"))
            .putString("credential", Base64.encodeToString(cipher.iv + cipher.doFinal(credential.toByteArray()), Base64.NO_WRAP))
            .putString("deviceId", deviceId())
            .apply()
    }

    fun read(): Pairing? {
        val endpoint = preferences.getString("endpoint", null) ?: return null
        val encrypted = preferences.getString("credential", null) ?: return null
        return runCatching {
            val bytes = Base64.decode(encrypted, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12))) }
            Pairing(endpoint, String(cipher.doFinal(bytes.copyOfRange(12, bytes.size))), deviceId())
        }.getOrNull()
    }

    private fun deviceId(): String = preferences.getString("deviceId", null) ?: UUID.randomUUID().toString().also {
        preferences.edit().putString("deviceId", it).apply()
    }

    private fun key(): javax.crypto.SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(alias, null) as? javax.crypto.SecretKey
        if (existing != null) return existing
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
}

data class Pairing(val endpoint: String, val credential: String, val deviceId: String)
