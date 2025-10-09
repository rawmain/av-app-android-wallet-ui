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
package eu.europa.ec.passportscanner.utils.extension

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint


// extension function to change bitmap contrast
fun Bitmap.setContrast(
    contrast: Float = 1.0F
): Bitmap? {
    val bitmap = copy(Bitmap.Config.ARGB_8888, true)
    val paint = Paint()

    // contrast 0..2, 1 is default
    // you may tweak the range
    val matrix = ColorMatrix(
        floatArrayOf(
            contrast, 0f, 0f, 0f, 0f,
            0f, contrast, 0f, 0f, 0f,
            0f, 0f, contrast, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )

    val filter = ColorMatrixColorFilter(matrix)
    paint.colorFilter = filter

    Canvas(bitmap).drawBitmap(this, 0f, 0f, paint)
    return bitmap
}

// extension function to change bitmap brightness
fun Bitmap.setBrightness(
    brightness: Float = 0.0F
): Bitmap? {
    val bitmap = copy(Bitmap.Config.ARGB_8888, true)
    val paint = Paint()

    // brightness -200..200, 0 is default
    // you may tweak the range
    val matrix = ColorMatrix(
        floatArrayOf(
            1.0F, 0f, 0f, 0f, brightness,
            0f, 1.0F, 0f, 0f, brightness,
            0f, 0f, 1.0F, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        )
    )

    val filter = ColorMatrixColorFilter(matrix)
    paint.colorFilter = filter

    Canvas(bitmap).drawBitmap(this, 0f, 0f, paint)
    return bitmap
}
