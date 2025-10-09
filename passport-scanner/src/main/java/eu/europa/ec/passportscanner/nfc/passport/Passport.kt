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
package eu.europa.ec.passportscanner.nfc.passport

import android.graphics.Bitmap
import android.os.Parcelable
import eu.europa.ec.passportscanner.nfc.details.AdditionalPersonDetails
import eu.europa.ec.passportscanner.nfc.details.PersonDetails
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import org.jmrtd.FeatureStatus
import org.jmrtd.VerificationStatus
import org.jmrtd.lds.SODFile

@Parcelize
class Passport(
    var sodFile: @RawValue SODFile? = null,
    var face: Bitmap? = null,
    var portrait: Bitmap? = null,
    var rawFaceImageData: @RawValue PassportNfcUtils.RawImageData? = null,
    var personDetails: PersonDetails? = null,
    var additionalPersonDetails: AdditionalPersonDetails? = null,
    var featureStatus: FeatureStatus? = FeatureStatus(),
    var verificationStatus: VerificationStatus? = VerificationStatus()
) : Parcelable