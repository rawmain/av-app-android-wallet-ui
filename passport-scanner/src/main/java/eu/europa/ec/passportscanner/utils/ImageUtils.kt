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
import android.graphics.BitmapFactory
import org.jnbis.internal.WsqDecoder
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream


object ImageUtils {

    var JPEG2000_MIME_TYPE = "image/jp2"
    var JPEG2000_ALT_MIME_TYPE = "image/jpeg2000"
    var WSQ_MIME_TYPE = "image/x-wsq"

    @Throws(IOException::class)
    fun decodeImage(inputStream: InputStream, imageLength: Int, mimeType: String): Bitmap {
        Timber.d("decodeImage of type $mimeType and length $imageLength")
        var byteInputStream = inputStream
        synchronized(byteInputStream) {
            val dataIn = DataInputStream(byteInputStream)
            val bytes = ByteArray(imageLength)
            dataIn.readFully(bytes)
            byteInputStream = ByteArrayInputStream(bytes)
        }
        if (JPEG2000_MIME_TYPE.equals(mimeType, ignoreCase = true) || JPEG2000_ALT_MIME_TYPE.equals(mimeType, ignoreCase = true)) {
            val bitmap = org.jmrtd.jj2000.JJ2000Decoder.decode(byteInputStream)
            return toAndroidBitmap(bitmap)
        } else if (WSQ_MIME_TYPE.equals(mimeType, ignoreCase = true)) {
            val wsqDecoder = WsqDecoder()
            val bitmap = wsqDecoder.decode(byteInputStream.readBytes())
            val byteData = bitmap.pixels
            val intData = IntArray(byteData.size)
            for (j in byteData.indices) {
                intData[j] = -0x1000000 or ((byteData[j].toInt() and 0xFF) shl 16) or ((byteData[j].toInt() and 0xFF) shl 8) or (byteData[j].toInt() and 0xFF)
            }
            return Bitmap.createBitmap(intData, 0, bitmap.width, bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        } else {
            inputStream.reset()
            try {
                return BitmapFactory.decodeStream(inputStream)
            }
            catch ( e: Exception) {
                Timber.e(e, "Failed to decode image using BitmapFactory, trying to read as JPEG2000")
                inputStream.reset()
                return decodeImage(inputStream, imageLength, JPEG2000_MIME_TYPE)
            }
        }
    }

    private fun toAndroidBitmap(bitmap: org.jmrtd.jj2000.Bitmap): Bitmap {
        val intData = bitmap.pixels
        return Bitmap.createBitmap(intData, 0, bitmap.width, bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    }
}
