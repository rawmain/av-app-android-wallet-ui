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

import timber.log.Timber
import java.io.Serializable

/**
 * Holds a MRZ date type.
 * @author Martin Vysny
 */
class MrzDate : Serializable, Comparable<MrzDate?> {
    /**
     * Year, 00-99.
     *
     *
     * Note: I am unable to find a specification of conversion of this value to a full year value.
     */
    val year: Int

    /**
     * Month, 1-12.
     */
    val month: Int

    /**
     * Day, 1-31.
     */
    val day: Int


    /**
     * Returns the date validity
     * @return Returns a boolean true if the parsed date is valid, false otherwise
     */
    /**
     * Is the date valid or not
     */
    val isDateValid: Boolean

    constructor(year: Int, month: Int, day: Int) {
        this.year = year
        this.month = month
        this.day = day
        this.isDateValid = check()
    }


    override fun toString(): String {
        return "{$day/$month/$year}"
    }


    private fun check(): Boolean {
        if (year < 0 || year > 99) {
            Timber.d("Parameter year: invalid value %s: must be 0..99", year)
            return false
        }
        if (month < 1 || month > 12) {
            Timber.d("Parameter month: invalid value %s: must be 1..12", month)
            return false
        }
        if (day < 1 || day > 31) {
            Timber.d("Parameter day: invalid value %s: must be 1..31", day)
            return false
        }

        return true
    }

    override fun equals(obj: Any?): Boolean {
        if (obj == null) {
            return false
        }
        if (javaClass != obj.javaClass) {
            return false
        }
        val other = obj as MrzDate
        if (this.year != other.year) {
            return false
        }
        if (this.month != other.month) {
            return false
        }
        return this.day == other.day
    }

    override fun hashCode(): Int {
        var hash = 7
        hash = 11 * hash + this.year
        hash = 11 * hash + this.month
        hash = 11 * hash + this.day
        return hash
    }

    override fun compareTo(o: MrzDate?): Int {
        if (o == null) {
            return 1
        }
        return year * 10000 + month * 100 + day.compareTo(o.year * 10000 + o.month * 100 + o.day)
    }
}
