/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2018  The JMRTD team
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 */
package org.jmrtd

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Security features of this identity document.
 *
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: 1559 $
 */
@Parcelize
class FeatureStatus(
    private var hasSAC: Verdict? = Verdict.UNKNOWN,
    private var hasBAC: Verdict? = Verdict.UNKNOWN,
    private var hasEAC: Verdict? = Verdict.UNKNOWN,
    private var hasCA: Verdict? = Verdict.UNKNOWN
) : Parcelable {

    fun setSAC(hasSAC: Verdict) {
        this.hasSAC = hasSAC
    }

    fun hasSAC(): Verdict? {
        return hasSAC
    }


    fun setBAC(hasBAC: Verdict) {
        this.hasBAC = hasBAC
    }

    fun hasBAC(): Verdict? {
        return hasBAC
    }


    fun setEAC(hasEAC: Verdict) {
        this.hasEAC = hasEAC
    }

    fun hasEAC(): Verdict? {
        return hasEAC
    }

    fun setCA(hasCA: Verdict) {
        this.hasCA = hasCA
    }

    fun hasCA(): Verdict? {
        return hasCA
    }

    fun summary(ident:String): String {
        return "$ident features: hasSAC = $hasSAC, hasBAC = $hasBAC, hasEAC = $hasEAC, hasCA = $hasCA"
    }

}