/*
 * Copyright (c) 2023 European Commission
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

package eu.europa.ec.passportscanner.parser.records

import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.passportscanner.parser.MrzParser
import eu.europa.ec.passportscanner.parser.MrzRange
import eu.europa.ec.passportscanner.parser.MrzRecord
import eu.europa.ec.passportscanner.parser.types.MrzFormat

/**
 * MRTD TD1 format: A three line long, 30 characters per line format.
 */
class MrtdTd1 : MrzRecord(MrzFormat.MRTD_TD1) {
    /**
     * Optional data at the discretion
     * of the issuing State. May contain
     * an extended document number
     * as per 6.7, note (j).
     */
    var optional: String? = null

    /**
     * optional (for U.S. passport holders, 21-29 may be corresponding passport number)
     */
    var optional2: String? = null

    override fun fromMrz(mrz: String, logController: LogController) {
        super.fromMrz(mrz, logController)
        val p = MrzParser(mrz, logController)
        documentNumber = p.parseString(MrzRange(5, 14, 0))
        validDocumentNumber = p.checkDigit(14, 0, MrzRange(5, 14, 0), "document number")
        optional = p.parseString(MrzRange(15, 30, 0))
        dateOfBirth = p.parseDate(MrzRange(0, 6, 1))
        validDateOfBirth =
            p.checkDigit(6, 1, MrzRange(0, 6, 1), "date of birth") && dateOfBirth.isDateValid
        sex = p.parseSex(7, 1)
        expirationDate = p.parseDate(MrzRange(8, 14, 1))
        validExpirationDate = p.checkDigit(
            14,
            1,
            MrzRange(8, 14, 1),
            "expiration date"
        ) && expirationDate.isDateValid
        nationality = p.parseString(MrzRange(15, 18, 1))
        optional2 = p.parseString(MrzRange(18, 29, 1))
        validComposite = p.checkDigit(
            29,
            1,
            p.rawValue(
                MrzRange(5, 30, 0),
                MrzRange(0, 7, 1),
                MrzRange(8, 15, 1),
                MrzRange(18, 29, 1)
            ),
            "mrz"
        )

        setName(p.parseName(MrzRange(0, 30, 2)))
    }

    override fun toString(): String {
        return "MRTD-TD1{" + super.toString() + ", optional=" + optional + ", optional2=" + optional2 + '}'
    }
}
