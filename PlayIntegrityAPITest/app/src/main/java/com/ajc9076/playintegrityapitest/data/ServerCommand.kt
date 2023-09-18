package com.ajc9076.playintegrityapitest.data

import kotlinx.serialization.Serializable

@Serializable
data class ServerCommand(
    val commandString: String,
    val tokenString: String
)