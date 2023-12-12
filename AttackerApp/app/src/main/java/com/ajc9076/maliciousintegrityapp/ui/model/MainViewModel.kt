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
    // object used to organize multiple coroutines on a single instance
    private val integrityChecker = CheckIntegrity()

    // set up state in our program
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<MainViewState> = integrityChecker.serverState.mapLatest {
        MainViewState(it)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = MainViewState(ServerState())
    )

    // launch a coroutine to verify the integrity of the app
    fun performCommand() {
        viewModelScope.launch {
            integrityChecker.computeResultAndParse()
        }
    }

    // launch a coroutine to wait for location data to come in
    fun waitForLocation(locationString: String) {
        viewModelScope.launch {
            integrityChecker.waitForLocation(locationString)
        }
    }
}