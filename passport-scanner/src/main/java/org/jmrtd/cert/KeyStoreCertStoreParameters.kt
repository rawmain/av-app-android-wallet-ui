/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2013  The JMRTD team
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

package org.jmrtd.cert

import java.security.KeyStore
import java.security.cert.CertStoreParameters

/**
 * Parameters for key store backed certificate store.
 *
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: $
 */
class KeyStoreCertStoreParameters(val keyStore: KeyStore) : Cloneable, CertStoreParameters {

    /**
     * Makes a shallow copy of this object as this
     * class is immutable.
     *
     * @return a shallow copy of this object
     */
    override fun clone(): Any {
        return KeyStoreCertStoreParameters(keyStore)
    }
}