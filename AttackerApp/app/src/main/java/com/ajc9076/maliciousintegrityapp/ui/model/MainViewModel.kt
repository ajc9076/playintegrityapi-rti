package com.ajc9076.maliciousintegrityapp.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajc9076.maliciousintegrityapp.data.ServerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel: ViewModel() {
    private val integrityChecker = CheckIntegrity()
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<MainViewState> = integrityChecker.serverState.mapLatest {
        MainViewState(it)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = MainViewState(ServerState())
    )

    fun performCommand() {
        viewModelScope.launch {
            integrityChecker.computeResultAndParse()
        }
    }

    fun waitForLocation(locationString: String) {
        viewModelScope.launch {
            integrityChecker.waitForLocation(locationString)
        }
    }
}