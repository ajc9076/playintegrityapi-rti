package com.ajc9076.maliciousintegrityapp.ui.model

import kotlinx.serialization.Serializable

@Serializable
data class ServerCommand(
    val commandString: String,
    val tokenString: String
)