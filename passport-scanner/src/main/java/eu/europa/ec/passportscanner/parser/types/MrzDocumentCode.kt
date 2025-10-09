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
package eu.europa.ec.passportscanner.parser.types

import eu.europa.ec.passportscanner.parser.MrzParseException
import eu.europa.ec.passportscanner.parser.MrzRange


/**
 * Lists all supported MRZ record types (a.k.a. document codes).
 * @author Martin Vysny
 */
enum class MrzDocumentCode {
    /**
     * A passport, P or IP.
     * ... maybe Travel Document that is very similar to the passport.
     */
    Passport,

    /**
     * General I type (besides IP).
     */
    TypeI,

    /**
     * General A type (besides AC).
     */
    TypeA,

    /**
     * Crew member (AC).
     */
    CrewMember,

    /**
     * General type C.
     */
    TypeC,

    /**
     * Type V (Visa).
     */
    TypeV,

    /**
     *
     */
    Migrant;

    companion object {
        /**
         * @author Zsombor
         * turning to switch statement due to lots of types
         *
         * @param mrz string
         */
        @JvmStatic
        fun parse(mrz: String): MrzDocumentCode {
            val code = mrz.substring(0, 2)

            // 2-letter checks
            when (code) {
                "IV" -> throw MrzParseException(
                    "IV document code is not allowed",
                    mrz,
                    MrzRange(0, 2, 0),
                    null
                ) // TODO why?

                "AC" -> return CrewMember
                "ME" -> return Migrant
                "TD" -> return Migrant // travel document
                "IP" -> return Passport
            }

            // 1-letter checks
            when (code.get(0)) {
                'T', 'P' -> return Passport
                'A' -> return TypeA
                'C' -> return TypeC
                'V' -> return TypeV
                'I' -> return TypeI // identity card or residence permit
                'R' -> return Migrant // swedish '51 Convention Travel Document
            }


            throw MrzParseException(
                "Unsupported document code: $code",
                mrz,
                MrzRange(0, 2, 0),
                null
            )
        }
    }
}
