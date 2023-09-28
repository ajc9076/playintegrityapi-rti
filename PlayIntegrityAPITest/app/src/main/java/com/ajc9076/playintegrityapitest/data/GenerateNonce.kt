package com.ajc9076.playintegrityapitest.data

import android.util.Log
import java.security.MessageDigest

fun ByteArray.toHexString(): String = joinToString(separator = "") {
        currentByte -> "%02x".format(currentByte)
}

object GenerateNonce {
    private const val TAG = "PlayIntegrityAPITest"

    // Generate a nonce for Play Integrity using the following steps:
    // 1. Generate a SHA-256 hash of the command string
    // 2. Convert the hash value to a hex string
    // 3. Take the random number string from the server and append the hash
    // hex string to it to create the nonce string
    // Play Integrity expects a URL encoded, non-padded Base64 string,
    // our hex string is a valid Base64 string, even though we don't actually
    // need to encode/decode it.
    fun generateNonceString(commandString: String, randomString: String) : String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val commandHashBytes = messageDigest.digest(
            commandString.toByteArray(Charsets.UTF_8))
        val commandHashString = commandHashBytes.toHexString()
        val nonceString = randomString + commandHashString
        Log.d(TAG, "nonce: $nonceString")
        return nonceString
    }
}