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

import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.passportscanner.nfc.passport.Passport
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
            logController: LogController,
        ): NFCResult {
            val personDetails = passport?.personDetails
            val additionalPersonDetails = passport?.additionalPersonDetails

            // Get proper date of birth
            val dateOfBirth = if (additionalPersonDetails?.fullDateOfBirth.isNullOrEmpty()) {
                formatStandardDate(
                    personDetails?.dateOfBirth,
                    threshold = BIRTH_DATE_THRESHOLD,
                    logController = logController
                )
            } else formatStandardDate(
                additionalPersonDetails.fullDateOfBirth,
                "yyyyMMdd",
                logController = logController
            )
            return NFCResult(
                dateOfExpiry = formatStandardDate(
                    personDetails?.dateOfExpiry,
                    threshold = EXPIRY_DATE_THRESHOLD,
                    logController = logController
                ),
                dateOfBirth = dateOfBirth,
            )
        }

    }
}
