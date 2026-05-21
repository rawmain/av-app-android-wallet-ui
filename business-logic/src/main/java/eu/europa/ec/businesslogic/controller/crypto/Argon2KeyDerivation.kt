/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.businesslogic.controller.crypto

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

object Argon2KeyDerivation {
    const val M_COST_KIB = 65_536
    const val T_COST = 3
    const val PARALLELISM = 1
    const val OUT_LEN = 32
    const val SALT_LEN = 32

    fun derive(
        pin: CharArray,
        salt: ByteArray,
        mCostKib: Int = M_COST_KIB,
        tCost: Int = T_COST,
        parallelism: Int = PARALLELISM,
    ): ByteArray {
        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withSalt(salt)
            .withMemoryAsKB(mCostKib)
            .withIterations(tCost)
            .withParallelism(parallelism)
            .build()
        val output = ByteArray(OUT_LEN)
        Argon2BytesGenerator().apply { init(params) }.generateBytes(pin, output)
        return output
    }
}
