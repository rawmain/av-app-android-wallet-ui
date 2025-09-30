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

import android.util.Log
import eu.europa.ec.passportscanner.parser.MrzParseException
import eu.europa.ec.passportscanner.parser.MrzRange
import eu.europa.ec.passportscanner.parser.MrzRecord
import eu.europa.ec.passportscanner.parser.records.MRP
import eu.europa.ec.passportscanner.parser.records.MrtdTd1


/**
 * Lists all supported MRZ formats.
 * @author Martin Vysny, Pierrick Martin
 */
enum class MrzFormat(
    val rows: Int,
    val columns: Int,
    private val recordClass: Class<out MrzRecord>
) {
    /**
     * MRTD td1 format: A three line long, 30 characters per line format.
     */
    MRTD_TD1(3, 30, MrtdTd1::class.java),

    /**
     * MRP Passport format: A two line long, 44 characters per line format.
     */
    PASSPORT(2, 44, MRP::class.java);

    /**
     * Checks if this format is able to parse given serialized MRZ record.
     * @param mrzRows MRZ record, separated into rows.
     * @return true if given MRZ record is of this type, false otherwise.
     */
    fun isFormatOf(mrzRows: Array<String?>): Boolean {
        return rows == mrzRows.size && columns == mrzRows[0]!!.length
    }

    /**
     * Creates new record instance with this type.
     * @return never null record instance.
     */
    fun newRecord(): MrzRecord {
        try {
            return recordClass.newInstance()
        } catch (ex: Exception) {
            throw RuntimeException(ex)
        }
    }

    companion object {
        /**
         * Detects given MRZ format.
         * @param mrz the MRZ string.
         * @return the format, never null.
         */
        @JvmStatic
        fun get(mrz: String): MrzFormat {
            var mrz = mrz
            val dummyRow = 44
            var rows: Array<String?> =
                mrz.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val cols = rows[0]!!.length
            Log.d("SmartScanner", "mrz: " + mrz)
            Log.d("SmartScanner", "rows: " + rows.contentToString())
            val mrzBuilder = StringBuilder(mrz)
            for (i in 1..<rows.size) {
                if (rows[i]!!.length != cols) {
                    //throw new MrzParseException("Different row lengths: 0: " + cols + " and " + i + ": " + rows[i].length(), mrz, new MrzRange(0, 0, 0), null);
                    if (rows[i]!!.length != dummyRow) {
                        mrzBuilder.append("<")
                    }
                }
            }
            mrz = mrzBuilder.toString()
            rows = mrz.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            Log.d("SmartScanner", "mrz append: " + mrz)
            Log.d("SmartScanner", "rows append: " + rows.contentToString())
            for (f in MrzFormat.entries) {
                if (f.isFormatOf(rows)) {
                    return f
                }
            }
            throw MrzParseException(
                "Unknown format / unsupported number of cols/rows: " + cols + "/" + rows.size,
                mrz,
                MrzRange(0, 0, 0),
                null
            )
        }
    }
}
