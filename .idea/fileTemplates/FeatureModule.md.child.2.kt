package eu.europa.ec.${NAME}feature.ui.screen

import androidx.navigation.NavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.${NAME}feature.viewmodel.${SCREEN_NAME}ViewModel

@Composable
fun ${SCREEN_NAME}Screen(navController: NavController, viewModel: ${SCREEN_NAME}ViewModel) {
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()
    Content()
    
    OneTimeLaunchedEffect {
        viewModel.setEvent(Event.Initialize)
    }
}

@Composable
private fun Content() {
}

@ThemeModePreviews
@Composable
private fun ${SCREEN_NAME}ScreenPreview() {
    PreviewTheme {
        Content()
    }
}