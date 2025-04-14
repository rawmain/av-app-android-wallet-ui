/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package eu.europa.ec.landingfeature.ui.dashboard

import android.net.Uri
import eu.europa.ec.commonfeature.config.OfferUiConfig
import eu.europa.ec.commonfeature.config.PresentationMode
import eu.europa.ec.commonfeature.config.QrScanFlow
import eu.europa.ec.commonfeature.config.QrScanUiConfig
import eu.europa.ec.commonfeature.config.RequestUriConfig
import eu.europa.ec.corelogic.di.getOrCreatePresentationScope
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.helper.DeepLinkAction
import eu.europa.ec.uilogic.navigation.helper.DeepLinkType
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.navigation.helper.hasDeepLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import org.koin.android.annotation.KoinViewModel

data class State(
    val isLoading: Boolean = false,
) : ViewState

sealed class Event : ViewEvent {
    data class Init(val deepLinkUri: Uri?) : Event()
    data object GoToSettings : Event()
    data object GoToScanQR : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data class SwitchScreen(
            val screenRoute: String,
            val popUpToScreenRoute: String = DashboardScreens.Dashboard.screenRoute,
            val inclusive: Boolean = false,
        ) : Navigation()

        data object Pop : Navigation()
        data class OpenDeepLinkAction(val deepLinkUri: Uri, val arguments: String?) : Navigation()
    }
}

@KoinViewModel
class LandingViewModel(
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
) : MviViewModel<Event, State, Effect>() {

    override fun setInitialState(): State {
        return State()
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                handleDeepLink(event.deepLinkUri)
            }

            is Event.GoToSettings -> {
                // No effect for now
            }

            is Event.GoToScanQR -> {
                navigateToQrScan()
            }
        }
    }

    private fun navigateToQrScan() {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = generateComposableNavigationLink(
                    screen = CommonScreens.QrScan,
                    arguments = generateComposableArguments(
                        mapOf(
                            QrScanUiConfig.serializedKeyName to uiSerializer.toBase64(
                                QrScanUiConfig(
                                    title = resourceProvider.getString(R.string.presentation_qr_scan_title),
                                    subTitle = resourceProvider.getString(R.string.presentation_qr_scan_subtitle),
                                    qrScanFlow = QrScanFlow.Presentation
                                ),
                                QrScanUiConfig.Parser
                            )
                        )
                    )
                )
            )
        }
    }

    private fun handleDeepLink(deepLinkUri: Uri?) {
        deepLinkUri?.let { uri ->
            hasDeepLink(uri)?.let {
                val arguments: String? = when (it.type) {
                    DeepLinkType.OPENID4VP -> generatePresentationDeepLinkArguments(uri)

                    DeepLinkType.CREDENTIAL_OFFER -> generateOfferDeepLinkArguments(it)

                    else -> null
                }
                setEffect {
                    Effect.Navigation.OpenDeepLinkAction(
                        deepLinkUri = uri,
                        arguments = arguments
                    )
                }
            }
        }
    }

    private fun generateOfferDeepLinkArguments(it: DeepLinkAction): String =
        generateComposableArguments(
            mapOf(
                OfferUiConfig.serializedKeyName to uiSerializer.toBase64(
                    OfferUiConfig(
                        offerURI = it.link.toString(),
                        onSuccessNavigation = ConfigNavigation(
                            navigationType = NavigationType.PopTo(
                                screen = DashboardScreens.Dashboard
                            )
                        ),
                        onCancelNavigation = ConfigNavigation(
                            navigationType = NavigationType.Pop
                        )
                    ),
                    OfferUiConfig
                )
            )
        )

    private fun generatePresentationDeepLinkArguments(uri: Uri): String {
        getOrCreatePresentationScope()
        return generateComposableArguments(
            mapOf(
                RequestUriConfig.serializedKeyName to uiSerializer.toBase64(
                    RequestUriConfig(
                        PresentationMode.OpenId4Vp(
                            uri.toString(),
                            DashboardScreens.Dashboard.screenRoute
                        )
                    ),
                    RequestUriConfig
                )
            )
        )
    }
}