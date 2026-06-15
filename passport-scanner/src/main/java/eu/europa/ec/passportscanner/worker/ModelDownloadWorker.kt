/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.passportscanner.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.passportscanner.face.FaceMatchModelSource
import eu.europa.ec.passportscanner.face.ModelDownloader
import eu.europa.ec.resourceslogic.R
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ModelDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val logController: LogController by inject()

    override suspend fun doWork(): Result {
        val embeddingUrl = inputData.getString(KEY_INPUT_EMBEDDING_URL)
        val embeddingSha256 = inputData.getString(KEY_INPUT_EMBEDDING_SHA256)

        if (embeddingUrl == null || embeddingSha256 == null) {
            return Result.failure(workDataOf(KEY_ERROR to "Missing embedding model configuration"))
        }

        setForeground(createForegroundInfo(0))

        val modelDownloader = ModelDownloader(applicationContext, logController)
        val destDir = applicationContext.filesDir.absolutePath
        val embeddingSource = FaceMatchModelSource.Remote(embeddingUrl, embeddingSha256)

        val embedding = modelDownloader.prepareModel(
            source = embeddingSource,
            destDir = destDir,
            outputFilename = EMBEDDING_OUTPUT_FILENAME,
        ) { progress ->
            setProgressAsync(workDataOf(KEY_PROGRESS to progress))
            setForegroundAsync(createForegroundInfo(progress))
        }

        if (embedding == null) {
            return Result.failure(workDataOf(KEY_ERROR to "Failed to download embedding model"))
        }

        return Result.success(workDataOf(KEY_EMBEDDING to embedding))
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_id)
            .setContentTitle(applicationContext.getString(R.string.passport_live_video_downloading))
            .setContentText(if (progress > 0) "$progress%" else "")
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            applicationContext.getString(R.string.passport_live_video_download_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        applicationContext.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    companion object {
        const val WORK_NAME = "modelDownload"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"
        const val KEY_EMBEDDING = "embedding"
        const val KEY_INPUT_EMBEDDING_URL = "inputEmbeddingUrl"
        const val KEY_INPUT_EMBEDDING_SHA256 = "inputEmbeddingSha256"
        const val EMBEDDING_OUTPUT_FILENAME = "embedding.onnx"
        private const val CHANNEL_ID = "model_download"
        private const val NOTIFICATION_ID = 9001
    }
}
