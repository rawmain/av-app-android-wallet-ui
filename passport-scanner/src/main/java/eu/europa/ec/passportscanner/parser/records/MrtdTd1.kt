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

import eu.europa.ec.passportscanner.parser.MrzParser
import eu.europa.ec.passportscanner.parser.MrzRange
import eu.europa.ec.passportscanner.parser.MrzRecord
import eu.europa.ec.passportscanner.parser.types.MrzFormat
import org.slf4j.LoggerFactory


/**
 * MRTD TD1 format: A three line long, 30 characters per line format.
 * @author Martin Vysny
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

    override fun fromMrz(mrz: String) {
        val log = LoggerFactory.getLogger(MrzParser::class.java)
        super.fromMrz(mrz)
        val p = MrzParser(mrz)
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
        log.debug(
            p.rawValue(
                MrzRange(5, 30, 0),
                MrzRange(0, 7, 1),
                MrzRange(8, 15, 1),
                MrzRange(18, 29, 1)
            )
        )
        setName(p.parseName(MrzRange(0, 30, 2)))
    }

    override fun toString(): String {
        return "MRTD-TD1{" + super.toString() + ", optional=" + optional + ", optional2=" + optional2 + '}'
    }


    companion object {
        private const val serialVersionUID = 1L
    }
}
