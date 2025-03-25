package eu.europa.ec.${NAME}feature.viewmodel

import androidx.lifecycle.ViewModel
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.${NAME}feature.interactor.${SCREEN_NAME}Interactor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.android.annotation.KoinViewModel

data class State(

) : ViewState

sealed class Event : ViewEvent {
    data object Initialize : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data class SwitchScreen(val route: String) : Navigation()
    }
}

@KoinViewModel
class ${SCREEN_NAME}ViewModel (
    private val interactor: ${SCREEN_NAME}Interactor
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State = State()

    override fun handleEvents(event: Event) {
        when (event) {
            Event.Initialize -> onScreenLoad()
        }
    }

    private fun onScreenLoad() {
        viewModelScope.launch {
            val screenRoute = interactor.getNextRoute()
            setEffect {
                Effect.Navigation.SwitchScreen(screenRoute)
            }
        }
    }
}
