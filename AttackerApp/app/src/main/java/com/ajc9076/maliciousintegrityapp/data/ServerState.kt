package com.ajc9076.maliciousintegrityapp.data

data class ServerState (
    var status: ServerStatus = ServerStatus.INIT,
    var verdict: String = "",
    var success: Boolean = false
)