/*
 * Java parser for the MRZ records, as specified by the ICAO organization.
 * Copyright (C) 2011 Innovatrics s.r.o.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package eu.europa.ec.passportscanner.parser.records

import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.passportscanner.parser.MrzParser
import eu.europa.ec.passportscanner.parser.MrzRange
import eu.europa.ec.passportscanner.parser.MrzRecord
import eu.europa.ec.passportscanner.parser.types.MrzFormat


/**
 * MRP Passport format: A two line long, 44 characters per line format.
 * @author Martin Vysny
 */
class MRP : MrzRecord(MrzFormat.PASSPORT) {
    /**
     * personal number (may be used by the issuing country as it desires), 14 characters long.
     */
    var personalNumber: String? = null

    override fun fromMrz(mrz: String, logController: LogController) {
        super.fromMrz(mrz, logController)
        val parser = MrzParser(mrz, logController)
        setName(parser.parseName(MrzRange(5, 44, 0)))
        documentNumber = parser.parseString(MrzRange(0, 9, 1))
        validDocumentNumber = parser.checkDigit(9, 1, MrzRange(0, 9, 1), "passport number")
        nationality = parser.parseString(MrzRange(10, 13, 1))
        dateOfBirth = parser.parseDate(MrzRange(13, 19, 1))
        validDateOfBirth = parser.checkDigit(
            19,
            1,
            MrzRange(13, 19, 1),
            "date of birth"
        ) && dateOfBirth.isDateValid
        sex = parser.parseSex(20, 1)
        expirationDate = parser.parseDate(MrzRange(21, 27, 1))
        validExpirationDate = parser.checkDigit(
            27,
            1,
            MrzRange(21, 27, 1),
            "expiration date"
        ) && expirationDate.isDateValid
        personalNumber = parser.parseString(MrzRange(28, 42, 1))
        validComposite = parser.checkDigit(
            43,
            1,
            parser.rawValue(MrzRange(0, 10, 1), MrzRange(13, 20, 1), MrzRange(21, 43, 1)),
            "mrz"
        )
    }

    override fun toString(): String {
        return "MRP{" + super.toString() + ", personalNumber=" + personalNumber + '}'
    }
}
