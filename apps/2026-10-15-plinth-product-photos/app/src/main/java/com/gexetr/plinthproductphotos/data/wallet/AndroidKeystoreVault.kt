package com.gexetr.plinthproductphotos.data.wallet

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.MessageDigest
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidKeystoreVault(
    private val alias: String,
) {
    fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(plaintext)
        return byteArrayOf(VERSION) + cipher.iv + encrypted
    }

    fun decrypt(payload: ByteArray): ByteArray {
        require(payload.size > IV_LENGTH + 1 && payload[0] == VERSION) {
            "Encrypted wallet data has an unsupported format"
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(TAG_LENGTH_BITS, payload.copyOfRange(1, IV_LENGTH + 1)),
        )
        return cipher.doFinal(payload.copyOfRange(IV_LENGTH + 1, payload.size))
    }

    fun encryptToBase64(plaintext: String): String =
        Base64.encodeToString(encrypt(plaintext.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)

    fun decryptBase64(ciphertext: String): String =
        decrypt(Base64.decode(ciphertext, Base64.NO_WRAP)).toString(Charsets.UTF_8)

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val VERSION: Byte = 1
        const val IV_LENGTH = 12
        const val TAG_LENGTH_BITS = 128
    }
}

internal fun sha256Hex(value: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
