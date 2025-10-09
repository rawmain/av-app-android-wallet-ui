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

/**
 * MRZ sex.
 * @author Martin Vysny
 */
enum class MrzSex(
    /**
     * The MRZ character.
     */
    val mrz: Char
) {
    Male('M'),
    Female('F'),
    Unspecified('X');

    companion object {
        @JvmStatic
        fun fromMrz(sex: Char): MrzSex {
            when (sex) {
                'M' -> return Male
                'F' -> return Female
                '<', 'X' -> return Unspecified
                else -> throw RuntimeException("Invalid MRZ sex character: " + sex)
            }
        }
    }
}
