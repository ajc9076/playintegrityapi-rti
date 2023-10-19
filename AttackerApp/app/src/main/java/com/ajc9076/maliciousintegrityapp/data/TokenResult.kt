package com.ajc9076.maliciousintegrityapp.data

import kotlinx.serialization.Serializable

@Serializable
data class TokenResult(
    val token: String,
    val commandString: String
)