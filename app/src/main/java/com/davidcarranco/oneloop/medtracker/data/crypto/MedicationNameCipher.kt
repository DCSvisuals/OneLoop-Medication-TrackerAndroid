package com.davidcarranco.oneloop.medtracker.data.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Device-bound AES key (Android Keystore) for local files, and an
 * account-derived key so iOS and Android encrypt Supabase rows the same way.
 */
class MedicationNameCipher(@Suppress("UNUSED_PARAMETER") context: Context) {
    private val deviceKey: SecretKey = DeviceKeyStore.loadOrCreate()

    fun encryptLocal(name: String): String = encryptWithKey(name, deviceKey)

    fun decryptLocal(name: String): String = decryptWithKey(name, deviceKey)

    fun encryptForAccount(name: String, userId: String): String =
        MedicationNameCrypto.encrypt(name, MedicationNameCrypto.accountKey(userId))

    fun decryptForAccount(name: String, userId: String): String =
        MedicationNameCrypto.decrypt(name, MedicationNameCrypto.accountKey(userId))

    private fun encryptWithKey(plaintext: String, key: SecretKey): String {
        if (plaintext.startsWith(MedicationNameCrypto.PREFIX)) return plaintext
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val nonce = cipher.iv
        val cipherText = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
        val packed = ByteBuffer.allocate(nonce.size + cipherText.size)
            .put(nonce)
            .put(cipherText)
            .array()
        return MedicationNameCrypto.PREFIX + Base64.encodeToString(packed, Base64.NO_WRAP)
    }

    private fun decryptWithKey(stored: String, key: SecretKey): String {
        if (!stored.startsWith(MedicationNameCrypto.PREFIX)) return stored
        val packed = runCatching {
            Base64.decode(stored.substring(MedicationNameCrypto.PREFIX.length), Base64.NO_WRAP)
        }.getOrNull() ?: return stored
        if (packed.size <= 12) return stored
        return runCatching {
            val nonce = packed.copyOfRange(0, 12)
            val cipherText = packed.copyOfRange(12, packed.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, nonce))
            String(cipher.doFinal(cipherText), StandardCharsets.UTF_8)
        }.getOrDefault(stored)
    }

    private object DeviceKeyStore {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ALIAS = "oneloop.medication-name.v1"

        fun loadOrCreate(): SecretKey {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey?.let { return it }
            val generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE,
            )
            generator.init(
                KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setKeySize(256)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            return generator.generateKey()
        }
    }
}
