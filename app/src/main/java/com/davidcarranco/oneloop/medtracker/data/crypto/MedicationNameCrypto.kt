package com.davidcarranco.oneloop.medtracker.data.crypto

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM for medication name strings.
 *
 * Wire format: `oln1.` + Base64(12-byte nonce || ciphertext || 16-byte tag).
 * Values without the prefix are treated as legacy plaintext.
 */
object MedicationNameCrypto {
    const val PREFIX = "oln1."
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128
    private val SALT = "OneLoop.medname.v1".toByteArray(StandardCharsets.UTF_8)
    private val INFO = "name-field".toByteArray(StandardCharsets.UTF_8)
    private val random = SecureRandom()

    fun accountKey(userId: String): ByteArray {
        val normalized = userId.trim().lowercase()
        return hkdfSha256(
            ikm = normalized.toByteArray(StandardCharsets.UTF_8),
            salt = SALT,
            info = INFO,
            length = 32,
        )
    }

    fun encrypt(plaintext: String, key: ByteArray): String {
        if (plaintext.startsWith(PREFIX)) return plaintext
        val nonce = ByteArray(NONCE_BYTES).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(TAG_BITS, nonce),
        )
        val cipherText = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
        val packed = ByteBuffer.allocate(nonce.size + cipherText.size)
            .put(nonce)
            .put(cipherText)
            .array()
        return PREFIX + java.util.Base64.getEncoder().encodeToString(packed)
    }

    fun decrypt(stored: String, key: ByteArray): String {
        if (!stored.startsWith(PREFIX)) return stored
        val packed = runCatching {
            java.util.Base64.getDecoder().decode(stored.substring(PREFIX.length))
        }.getOrNull() ?: return stored
        if (packed.size <= NONCE_BYTES) return stored
        return runCatching {
            val nonce = packed.copyOfRange(0, NONCE_BYTES)
            val cipherText = packed.copyOfRange(NONCE_BYTES, packed.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, "AES"),
                GCMParameterSpec(TAG_BITS, nonce),
            )
            String(cipher.doFinal(cipherText), StandardCharsets.UTF_8)
        }.getOrDefault(stored)
    }

    internal fun hkdfSha256(
        ikm: ByteArray,
        salt: ByteArray,
        info: ByteArray,
        length: Int,
    ): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val saltKey = if (salt.isEmpty()) ByteArray(mac.macLength) else salt
        mac.init(SecretKeySpec(saltKey, "HmacSHA256"))
        val prk = mac.doFinal(ikm)
        var previous = ByteArray(0)
        val out = ByteArrayOutputStream()
        var counter = 1
        while (out.size() < length) {
            mac.init(SecretKeySpec(prk, "HmacSHA256"))
            mac.update(previous)
            mac.update(info)
            mac.update(counter.toByte())
            previous = mac.doFinal()
            out.write(previous)
            counter++
        }
        return out.toByteArray().copyOf(length)
    }
}
