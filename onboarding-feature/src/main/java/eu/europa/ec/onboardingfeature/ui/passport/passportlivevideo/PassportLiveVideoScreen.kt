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

package eu.europa.ec.onboardingfeature.ui.passport.passportlivevideo

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.onboardingfeature.ui.passport.passportlivevideo.Effect.Navigation
import eu.europa.ec.passportscanner.face.AVFaceMatchSDK
import eu.europa.ec.passportscanner.face.AVFaceMatchSdkImpl
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.BulletHolder
import eu.europa.ec.uilogic.component.PassportVerificationStepBar
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.ButtonConfig
import eu.europa.ec.uilogic.component.wrap.ButtonType
import eu.europa.ec.uilogic.component.wrap.StickyBottomConfig
import eu.europa.ec.uilogic.component.wrap.StickyBottomType
import eu.europa.ec.uilogic.component.wrap.TextConfig
import eu.europa.ec.uilogic.component.wrap.WrapStickyBottomContent
import eu.europa.ec.uilogic.component.wrap.WrapText

@Composable
fun PassportLiveVideoScreen(
    controller: NavController,
    viewModel: PassportLiveVideoViewModel
) {

    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var faceMatchSdk by remember { mutableStateOf<AVFaceMatchSDK?>(null) }
    var isSdkInitializing by remember { mutableStateOf(false) }

    // Initialize SDK in background when screen loads
    LaunchedEffect(Unit) {
        Log.d("PassportLiveVideoScreen", "LaunchedEffect started")
        isSdkInitializing = true
        try {
            withContext(Dispatchers.IO) {
                Log.d("PassportLiveVideoScreen", "Entered IO dispatcher")
                Log.d("PassportLiveVideoScreen", "Creating AVFaceMatchSdkImpl...")
                val sdk = AVFaceMatchSdkImpl(context.applicationContext)
                Log.d("PassportLiveVideoScreen", "Loading config from assets...")
                val configJson = context.assets.open("keyless_config.json").bufferedReader().use { it.readText() }
                Log.d("PassportLiveVideoScreen", "Config loaded: ${configJson.take(100)}...")
                Log.d("PassportLiveVideoScreen", "Calling sdk.init()...")
                val success = sdk.init(configJson)
                Log.d("PassportLiveVideoScreen", "SDK initialization completed with result: $success")
                if (success) {
                    withContext(Dispatchers.Main) {
                        Log.d("PassportLiveVideoScreen", "Setting faceMatchSdk state...")
                        faceMatchSdk = sdk
                        Log.d("PassportLiveVideoScreen", "SDK is now ready! faceMatchSdk = $sdk")
                        Toast.makeText(context, "Face recognition ready!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("PassportLiveVideoScreen", "SDK init returned false - initialization failed")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "SDK initialization failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PassportLiveVideoScreen", "Exception during SDK initialization: ${e.javaClass.simpleName}: ${e.message}", e)
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to initialize SDK: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } finally {
            isSdkInitializing = false
            Log.d("PassportLiveVideoScreen", "SDK initialization process complete. isSdkInitializing = false")
        }
    }

    ContentScreen(
        isLoading = state.isLoading || isSdkInitializing,
        navigatableAction = ScreenNavigateAction.NONE,
        onBack = { viewModel.setEvent(Event.OnBackPressed) },
        stickyBottom = { paddingValues ->
            ActionButtons(
                paddings = paddingValues,
                onBack = { viewModel.setEvent(Event.OnBackPressed) },
                onLiveVideo = { viewModel.setEvent(Event.OnLiveVideoCapture) },
                isEnabled = !isSdkInitializing && faceMatchSdk != null
            )
        }
    ) { paddingValues -> Content(paddingValues = paddingValues) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is Navigation.GoBack -> controller.popBackStack()
                Navigation.StartVideoLiceCapture -> {
                    val sdk = faceMatchSdk
                    if (sdk != null) {
                        state.config?.let { config ->
                            startCapturing(context, sdk, config.faceImageTempPath)
                        }
                    } else {
                        Log.e("PassportLiveVideoScreen", "Cannot start capture: SDK not initialized")
                        Toast.makeText(context, "SDK not ready, please wait...", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}

fun startCapturing(context: Context, faceMatchSdk: AVFaceMatchSDK, faceImageTempPath: String) {
    Log.d("PassportLiveVideoScreen", "start capture & match using face image: $faceImageTempPath")

    // Use the face image from passport scanning
    faceMatchSdk.captureAndMatch(faceImageTempPath) { result ->
        Log.d("PassportLiveVideoScreen", "get result: $result")
        if (result.processed && result.capturedIsLive && result.isSameSubject) {
            Toast.makeText(context, "Same Person as passport -> Next Page", Toast.LENGTH_SHORT)
                .show()
        } else {
            Toast.makeText(context, "not matching -> Show Error", Toast.LENGTH_SHORT).show()
        }
        // TODO Clean up the temporary file after use
        //File(faceImageTempPath).delete()
    }
}

@Composable
private fun ActionButtons(
    onBack: () -> Unit,
    onLiveVideo: () -> Unit,
    paddings: PaddingValues,
    isEnabled: Boolean = true
) {

    val buttons = StickyBottomType.TwoButtons(
        primaryButtonConfig = ButtonConfig(
            type = ButtonType.SECONDARY,
            onClick = onBack
        ),
        secondaryButtonConfig = ButtonConfig(
            type = ButtonType.PRIMARY,
            onClick = onLiveVideo,
            enabled = isEnabled
        )
    )

    WrapStickyBottomContent(
        stickyBottomModifier = Modifier
            .fillMaxWidth()
            .padding(paddings),
        stickyBottomConfig = StickyBottomConfig(type = buttons, showDivider = false)
    ) { buttonConfigs ->
        buttonConfigs?.let { buttonConfig ->
            when (buttonConfig.type) {
                ButtonType.PRIMARY -> Text(stringResource(R.string.passport_live_video_live_capture))
                ButtonType.SECONDARY -> Text(stringResource(R.string.passport_live_video_back))
            }
        }
    }
}

@Composable
private fun Content(paddingValues: PaddingValues) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
    ) {

        PassportVerificationStepBar(2)

        VSpacer.ExtraLarge()
        WrapText(
            text = stringResource(R.string.passport_live_video_header),
            textConfig = TextConfig(
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                )
            ),
        )

        VSpacer.ExtraLarge()
        WrapText(
            text = stringResource(R.string.passport_live_video_description),
            textConfig = TextConfig(
                style = MaterialTheme.typography.bodyLarge
            )
        )

        VSpacer.Large()
        BulletHolder(
            stringResource(R.string.passport_live_video_step_first),
            stringResource(R.string.passport_live_video_step_second),
            stringResource(R.string.passport_live_video_step_third)
        )

        VSpacer.Large()
        WrapText(
            text = stringResource(R.string.passport_live_video_footer),
            textConfig = TextConfig(
                style = MaterialTheme.typography.bodyLarge,
                maxLines = Int.MAX_VALUE
            )
        )
    }
}

@Composable
@ThemeModePreviews
private fun PassportLiveVideoScreenPreview() {
    PreviewTheme {
        ContentScreen(
            isLoading = false,
            navigatableAction = ScreenNavigateAction.NONE,
            onBack = {},
            stickyBottom = { paddingValues ->
                ActionButtons(
                    paddings = paddingValues,
                    onBack = {},
                    onLiveVideo = {}
                )
            }
        ) { paddingValues -> Content(paddingValues = paddingValues) }
    }
}
