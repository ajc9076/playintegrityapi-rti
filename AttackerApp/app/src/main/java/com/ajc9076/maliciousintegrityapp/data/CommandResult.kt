package com.ajc9076.maliciousintegrityapp.data

import kotlinx.serialization.Serializable

@Serializable
data class CommandResult(
    val commandSuccess: Boolean,
    val diagnosticMessage: String,
    val expressToken: String
)