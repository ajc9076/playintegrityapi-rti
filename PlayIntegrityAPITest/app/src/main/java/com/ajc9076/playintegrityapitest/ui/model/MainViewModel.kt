package com.ajc9076.playintegrityapitest.ui.model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajc9076.playintegrityapitest.data.ServerState
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

    fun performCommand(context: Context, locationString: String) {
        viewModelScope.launch {
            integrityChecker.computeResultAndParse(context, locationString)
        }
    }
}