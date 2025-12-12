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
import eu.europa.ec.passportscanner.SmartScannerActivity.Companion.TAG
import eu.europa.ec.passportscanner.nfc.passport.Passport
import eu.europa.ec.passportscanner.parser.records.MrtdTd1
import eu.europa.ec.passportscanner.parser.types.MrzDate
import eu.europa.ec.passportscanner.utils.DateUtils
import eu.europa.ec.passportscanner.utils.DateUtils.BIRTH_DATE_THRESHOLD
import eu.europa.ec.passportscanner.utils.DateUtils.EXPIRY_DATE_THRESHOLD
import eu.europa.ec.passportscanner.utils.DateUtils.formatStandardDate
import java.util.Locale


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
                DateUtils.toAdjustedDate(
                    formatStandardDate(
                        personDetails?.dateOfBirth,
                        threshold = BIRTH_DATE_THRESHOLD,
                        logController = logController
                    )
                )
            } else formatStandardDate(
                additionalPersonDetails.fullDateOfBirth,
                "yyyyMMdd",
                logController = logController
            )
            return NFCResult(
                dateOfExpiry = DateUtils.toReadableDate(
                    formatStandardDate(
                        personDetails?.dateOfExpiry,
                        threshold = EXPIRY_DATE_THRESHOLD,
                        logController = logController
                    )
                ),
                dateOfBirth = dateOfBirth,
            )
        }

        fun fromEID(mrzRecord: MrtdTd1, logController: LogController): NFCResult {
            val dateOfBirth = formatMrzDateToStandard(mrzRecord.dateOfBirth, logController)
            val dateOfExpiry = formatMrzDateToStandard(mrzRecord.expirationDate, logController)

            return NFCResult(
                dateOfBirth = DateUtils.toAdjustedDate(
                    formatStandardDate(
                        dateOfBirth,
                        threshold = BIRTH_DATE_THRESHOLD,
                        logController = logController,
                    )
                ), dateOfExpiry = DateUtils.toReadableDate(
                    formatStandardDate(
                        dateOfExpiry,
                        threshold = EXPIRY_DATE_THRESHOLD,
                        logController = logController
                    )
                )
            )
        }

        private fun formatMrzDateToStandard(
            mrzDate: MrzDate,
            logController: LogController
        ): String? {
            return try {
                // Format MrzDate directly to yyMMdd format
                String.format(
                    Locale.US, "%02d%02d%02d", mrzDate.year, mrzDate.month, mrzDate.day
                )
            } catch (e: Exception) {
                logController.e(TAG, e) {
                    "Error formatting MRZ date:" + " ${mrzDate.day}/${mrzDate.month}/${mrzDate.year}"
                }
                null
            }
        }
    }
}
