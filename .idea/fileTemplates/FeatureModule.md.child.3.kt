#set($CAPITALIZED_MODULE = $NAME.substring(0,1).toUpperCase() + $NAME.substring(1) )
#set($CAPITALIZED_SCREEN = $SCREEN_NAME.toUpperCase() )
package eu.europa.ec.${NAME}feature.router
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import eu.europa.ec.${NAME}feature.ui.screen.${SCREEN_NAME}Screen
import eu.europa.ec.uilogic.navigation.ModuleRoute
import eu.europa.ec.uilogic.navigation.${CAPITALIZED_MODULE}Screens
import org.koin.androidx.compose.koinViewModel

fun NavGraphBuilder.feature${CAPITALIZED_MODULE}Graph(navController: NavController) {
    navigation(
        startDestination = ${CAPITALIZED_MODULE}Screens.${CAPITALIZED_SCREEN}.screenRoute,
        route = ModuleRoute.${CAPITALIZED_MODULE}Module.route
    ) {
        composable(route = ${CAPITALIZED_MODULE}Screens.${SCREEN_NAME}.screenRoute) {
            ${SCREEN_NAME}Screen(navController, koinViewModel())
        }
    }
}