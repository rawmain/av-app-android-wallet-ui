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
package eu.europa.ec.passportscanner.nfc

import eu.europa.ec.passportscanner.nfc.passport.Passport
import eu.europa.ec.passportscanner.utils.DateUtils
import eu.europa.ec.passportscanner.utils.DateUtils.BIRTH_DATE_THRESHOLD
import eu.europa.ec.passportscanner.utils.DateUtils.EXPIRY_DATE_THRESHOLD
import eu.europa.ec.passportscanner.utils.DateUtils.formatStandardDate


data class NFCResult(
    var dateOfBirth: String? = null,
    var dateOfExpiry: String? = null,
) {
    companion object {
        fun formatResult(
            passport: Passport?,
        ): NFCResult {
            val personDetails = passport?.personDetails
            val additionalPersonDetails = passport?.additionalPersonDetails
            // Note: In getting proper names
            // we split nameOfHolder to two parts separated by '<<' (double chevron)
            // one part of nameOfHolder should contain last names/surname
            // other part remaining of nameOfHolder should contain given names
            // in which multiple names are separated by '<' (single chevron)
            // Get proper date of birth
            val dateOfBirth = if (additionalPersonDetails?.fullDateOfBirth.isNullOrEmpty()) {
                DateUtils.toAdjustedDate(
                    formatStandardDate(
                        personDetails?.dateOfBirth,
                        threshold = BIRTH_DATE_THRESHOLD
                    )
                )
            } else formatStandardDate(additionalPersonDetails.fullDateOfBirth, "yyyyMMdd")
            return NFCResult(
                dateOfExpiry = DateUtils.toReadableDate(
                    formatStandardDate(
                        personDetails?.dateOfExpiry,
                        threshold = EXPIRY_DATE_THRESHOLD
                    )
                ),
                dateOfBirth = dateOfBirth,
            )
        }
    }
}