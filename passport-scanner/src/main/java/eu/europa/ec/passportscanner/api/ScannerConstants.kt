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
package eu.europa.ec.passportscanner.api


object ScannerConstants {

    const val MRZ = "mrz"

    // NFC
    const val NFC_MRZ_STRING = "nfc_mrz_string"
    const val EXPIRY_DATE = "expiry_date"
    const val DATE_OF_BIRTH = "date_of_birth"
    const val IS_PASSPORT = "is_passport"

    // Face Image
    const val NFC_FACE_IMAGE = "face_image"
    const val NFC_FACE_IMAGE_MIME_TYPE = "face_image_mime_type"
    const val NFC_FACE_IMAGE_LENGTH = "face_image_length"
}
