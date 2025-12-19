/*
 * Copyright (C) 2020 Newlogic Pte. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package eu.europa.ec.passportscanner.utils

import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Bitmap.createBitmap
import android.graphics.BitmapFactory
import eu.europa.ec.businesslogic.controller.log.LogController
import org.jnbis.internal.WsqDecoder
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream


object ImageUtils {

    var JPEG2000_MIME_TYPE = "image/jp2"
    var JPEG2000_ALT_MIME_TYPE = "image/jpeg2000"
    var WSQ_MIME_TYPE = "image/x-wsq"

    @Throws(IOException::class)
    fun decodeImage(
        inputStream: InputStream,
        imageLength: Int,
        mimeType: String,
        logController: LogController,
    ): Bitmap {
        logController.d { "decodeImage of type $mimeType and length $imageLength" }
        return when {
            JPEG2000_MIME_TYPE.equals(mimeType, ignoreCase = true) || JPEG2000_ALT_MIME_TYPE.equals(
                mimeType,
                ignoreCase = true
            ) -> {
                decodeJPEG2000(inputStream, imageLength)
            }

            WSQ_MIME_TYPE.equals(mimeType, ignoreCase = true) -> {
                decodeWSQ(inputStream, imageLength)
            }

            else -> {
                decodeDefault(logController, inputStream, imageLength)
            }
        }
    }

    @Throws(IOException::class)
    private fun readInputStreamToByteArray(
        inputStream: InputStream,
        imageLength: Int
    ): ByteArrayInputStream {
        return synchronized(inputStream) {
            val bytes = ByteArray(imageLength)
            DataInputStream(inputStream).readFully(bytes)
            ByteArrayInputStream(bytes)
        }
    }

    @Throws(IOException::class)
    private fun decodeJPEG2000(inputStream: InputStream, imageLength: Int): Bitmap {
        val byteInputStream = readInputStreamToByteArray(inputStream, imageLength)
        val bitmap = org.jmrtd.jj2000.JJ2000Decoder.decode(byteInputStream)
        val intData = bitmap.pixels
        return createBitmap(intData, 0, bitmap.width, bitmap.width, bitmap.height, Config.ARGB_8888)
    }

    @Throws(IOException::class)
    private fun decodeWSQ(inputStream: InputStream, imageLength: Int): Bitmap {
        val byteInputStream = readInputStreamToByteArray(inputStream, imageLength)
        val wsqDecoder = WsqDecoder()
        val bitmap = wsqDecoder.decode(byteInputStream.readBytes())
        val byteData = bitmap.pixels
        val intData = IntArray(byteData.size)
        for (j in byteData.indices) {
            intData[j] =
                -0x1000000 or ((byteData[j].toInt() and 0xFF) shl 16) or ((byteData[j].toInt() and 0xFF) shl 8) or (byteData[j].toInt() and 0xFF)
        }
        return createBitmap(
            intData,
            0,
            bitmap.width,
            bitmap.width,
            bitmap.height,
            Bitmap.Config.ARGB_8888
        )
    }

    @Throws(IOException::class)
    private fun decodeDefault(
        logController: LogController,
        inputStream: InputStream,
        imageLength: Int
    ): Bitmap {
        inputStream.reset()
        try {
            return BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            logController.e(e) { "Failed to decode using BitmapFactory, try to read as JP2" }
            inputStream.reset()
            return decodeJPEG2000(inputStream, imageLength)
        }
    }

}
