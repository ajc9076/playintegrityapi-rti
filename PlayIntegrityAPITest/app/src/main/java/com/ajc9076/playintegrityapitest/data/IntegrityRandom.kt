package com.ajc9076.playintegrityapitest.data

import kotlinx.serialization.Serializable

@Serializable
data class IntegrityRandom(
    val random: String,
    val timestamp: ULong
)