package com.ajc9076.playintegrityapitest.data

data class ServerState (
    var status: ServerStatus = ServerStatus.INIT1,
    var verdict: String = "",
    var success: Boolean = false
)