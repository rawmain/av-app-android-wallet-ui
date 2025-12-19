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
package eu.europa.ec.passportscanner.parser

import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.passportscanner.parser.types.MrzDate
import eu.europa.ec.passportscanner.parser.types.MrzDocumentCode
import eu.europa.ec.passportscanner.parser.types.MrzDocumentCode.Companion.parse
import eu.europa.ec.passportscanner.parser.types.MrzFormat
import eu.europa.ec.passportscanner.parser.types.MrzFormat.Companion.get
import eu.europa.ec.passportscanner.parser.types.MrzSex
import java.io.Serializable


/**
 * An abstract MRZ record, contains basic information present in all MRZ record types.
 * @author Martin Vysny
 */
abstract class MrzRecord protected constructor(
    /**
     * Detected MRZ format.
     */
    val format: MrzFormat?,
) : Serializable {
    /**
     * The document code.
     */
    var code: MrzDocumentCode? = null

    /**
     * Document code, see [MrzDocumentCode] for details on allowed values.
     */
    var code1: Char = 0.toChar()

    /**
     * For MRTD: Type, at discretion of states, but 1-2 should be IP for passport card, AC for crew member and IV is not allowed.
     * For MRP: Type (for countries that distinguish between different types of passports)
     */
    var code2: Char = 0.toChar()


    /**
     * An [ISO 3166-1 alpha-3](http://en.wikipedia.org/wiki/ISO_3166-1_alpha-3) country code of issuing country, with additional allowed values (according to [article on Wikipedia](http://en.wikipedia.org/wiki/Machine-readable_passport)):
     *  * D: Germany
     *  * GBD: British dependent territories citizen(note: the country code of the overseas territory is presently used to indicate issuing authority and nationality of BOTC)
     *  * GBN: British National (Overseas)
     *  * GBO: British Overseas citizen
     *  * GBP: British protected person
     *  * GBS: British subject
     *  * UNA: specialized agency of the United Nations
     *  * UNK: resident of Kosovo to whom a travel document has been issued by the United Nations Interim Administration Mission in Kosovo (UNMIK)
     *  * UNO: United Nations Organization
     *  * XOM: Sovereign Military Order of Malta
     *  * XXA: stateless person, as per the 1954 Convention Relating to the Status of Stateless Persons
     *  * XXB: refugee, as per the 1951 Convention Relating to the Status of Refugees
     *  * XXC: refugee, other than defined above
     *  * XXX: unspecified nationality
     */
    lateinit var issuingCountry: String

    /**
     * Document number, e.g. passport number.
     */
    lateinit var documentNumber: String

    /**
     * The surname in uppercase.
     */
    lateinit var surname: String

    /**
     * The given names in uppercase, separated by spaces.
     */
    var givenNames: String? = null

    /**
     * Date of birth.
     */
    lateinit var dateOfBirth: MrzDate

    /**
     * Sex
     */
    lateinit var sex: MrzSex

    /**
     * expiration date of passport
     */
    lateinit var expirationDate: MrzDate

    /**
     * An [ISO 3166-1 alpha-3](http://en.wikipedia.org/wiki/ISO_3166-1_alpha-3) country code of nationality.
     * See [.issuingCountry] for additional allowed values.
     */
    lateinit var nationality: String


    /**
     * check digits, usually common in every document.
     */
    var validDocumentNumber: Boolean = true
    var validDateOfBirth: Boolean = true
    var validExpirationDate: Boolean = true
    var validComposite: Boolean = true


    override fun toString(): String {
        return ("MrzRecord{" + "code=" + code + "[" + code1 + code2 + "], issuingCountry=" + issuingCountry + ", documentNumber=" + documentNumber
                + ", surname=" + surname + ", givenNames=" + givenNames + ", dateOfBirth=" + dateOfBirth + ", sex=" + sex + ", expirationDate="
                + expirationDate + ", nationality=" + nationality + '}')
    }

    /**
     * Parses the MRZ record.
     * @param mrz the mrz record, not null, separated by \n
     * @throws MrzParseException when a problem occurs.
     */
    @Throws(MrzParseException::class)
    open fun fromMrz(mrz: String, logController: LogController) {
        if (format != get(mrz, logController)) {
            throw MrzParseException(
                "invalid format: " + get(mrz, logController),
                mrz,
                MrzRange(0, 0, 0),
                format
            )
        }
        code = parse(mrz)
        code1 = mrz[0]
        code2 = mrz[1]
        issuingCountry = MrzParser(mrz, logController).parseString(MrzRange(2, 5, 0))
    }

    /**
     * Helper method to set the full name. Changes both [.surname] and [.givenNames].
     * @param name expected array of length 2, in the form of [surname, first_name]. Must not be null.
     */
    protected fun setName(name: Array<String?>) {
        surname =
            name[0] ?: throw MrzParseException("surname is null", "", MrzRange(0, 0, 0), format)
        givenNames = name[1]
    }

}
