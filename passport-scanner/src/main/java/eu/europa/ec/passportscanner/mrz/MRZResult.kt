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
package eu.europa.ec.passportscanner.mrz

import eu.europa.ec.passportscanner.utils.DateUtils
import eu.europa.ec.passportscanner.parser.MrzRecord
import eu.europa.ec.passportscanner.parser.records.MrtdTd1


data class MRZResult(
        val image: String?,
        val code: String?,
        val code1: Short?,
        val code2: Short?,
        val dateOfBirth: String?,
        val documentNumber: String?,
        val expirationDate: String?,
        val format: String?,
        val givenNames: String?,
        val issuingCountry: String?,
        val nationality: String?,
        val sex: String?,
        val surname: String?,
        val optional: String? = null,
        val optional2: String? = null,
        val validComposite: Boolean? = null
) {
    companion object {
        fun formatMrzResult(record: MrzRecord, image: String? = "") : MRZResult {
            val dateOfBirth = DateUtils.toAdjustedDate(record.dateOfBirth?.toString()?.replace(Regex("[{}]"), ""))
            val expirationDate = DateUtils.toReadableDate(record.expirationDate?.toString()?.replace(Regex("[{}]"), ""))
            return MRZResult(
                    image = image,
                    code = record.code.toString(),
                    code1 = record.code1.toShort(),
                    code2 = record.code2.toShort(),
                    dateOfBirth = dateOfBirth,
                    documentNumber = record.documentNumber.toString(),
                    expirationDate = expirationDate,
                    format = record.format.toString(),
                    givenNames = record.givenNames,
                    issuingCountry = record.issuingCountry,
                    nationality = record.nationality,
                    sex = record.sex.toString(),
                    surname = record.surname,
                    validComposite = record.validComposite
            )
        }

        fun formatMrtdTd1Result(record: MrtdTd1, image: String?) : MRZResult {
            val dateOfBirth = DateUtils.toAdjustedDate(record.dateOfBirth?.toString()?.replace(Regex("[{}]"), ""))
            val expirationDate = DateUtils.toReadableDate(record.expirationDate?.toString()?.replace(Regex("[{}]"), ""))
            return MRZResult(
                    image = image,
                    code = record.code.toString(),
                    code1 = record.code1.toShort(),
                    code2 = record.code2.toShort(),
                    dateOfBirth = dateOfBirth,
                    documentNumber = record.documentNumber.toString(),
                    expirationDate = expirationDate,
                    format = record.format.toString(),
                    givenNames = record.givenNames,
                    issuingCountry = record.issuingCountry,
                    nationality = record.nationality,
                    sex = record.sex.toString(),
                    surname = record.surname,
                    optional = record.optional,
                    optional2 = record.optional2,
                    validComposite = record.validComposite
            )
        }
    }
}
