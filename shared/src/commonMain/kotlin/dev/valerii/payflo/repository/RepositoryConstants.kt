package dev.valerii.payflo.repository

import io.ktor.util.encodeBase64

object RepositoryConstants {
    // "http://0.0.0.0:8080"
    // "http://10.0.2.2:8080"
    const val BASE_URL = "http://0.0.0.0:8080"
    const val KEY_USER_ID = "user_id"

    fun base64Encode(bytes: ByteArray) = bytes.encodeBase64()
}
