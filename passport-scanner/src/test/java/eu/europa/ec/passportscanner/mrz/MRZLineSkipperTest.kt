/*
 * Copyright (c) 2026 European Commission
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

package eu.europa.ec.passportscanner.mrz

import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.passportscanner.parser.MrzParseException
import eu.europa.ec.passportscanner.parser.MrzRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class MRZLineSkipperTest {

    private lateinit var logController: LogController
    private lateinit var mockRecord: MrzRecord
    private lateinit var skipper: MRZLineSkipper


    var callCount = 0
    val alwaysInvalidMRZCallback: (String, LogController) -> MrzRecord = { mrzString, _ ->
        callCount++
        // never succeed to get all combinations
        throw MrzParseException("Invalid MRZ", mrzString, null, null)
    }

    @Before
    fun setup() {
        logController = mock()
        mockRecord = mock()

        callCount = 0

        // Default valid record with all checksums valid
        whenever(mockRecord.validDateOfBirth).thenReturn(true)
        whenever(mockRecord.validDocumentNumber).thenReturn(true)
        whenever(mockRecord.validExpirationDate).thenReturn(true)
        whenever(mockRecord.validComposite).thenReturn(false)
    }

    @Test
    fun `tryParse with 3 lines starting with P should try 4 combinations`() {
        skipper = MRZLineSkipper(logController, alwaysInvalidMRZCallback)

        val result = skipper.tryParse("PLINE1\nPLINE2\nLINE3")

        assertNull(result)
        // Should try one 3-line combinations and three 2-line combinations
        assertTrue("Expected 4 parse attempts, got $callCount", callCount ==4)
    }

    //check the optimisation
    @Test
    fun `tryParse with 3 lines, not starting with P, should skip 2-line combinations`() {
        skipper = MRZLineSkipper(logController, alwaysInvalidMRZCallback)

        val result = skipper.tryParse("LINE1\nLINE2\nLINE3")

        assertNull(result)
        // Should try one 3-line combinations first and three 2-line combinations
        assertTrue("Expected 1 parse attempts, got $callCount", callCount ==1)
    }

    @Test
    fun `tryParse with 2 lines should parse directly`() {
        skipper = MRZLineSkipper(logController, alwaysInvalidMRZCallback)

        val result = skipper.tryParse("PLINE1\nLINE21")

        assertNull(result)
        assertEquals(1, callCount)
    }

    @Test
    fun `tryParse with single line should parse directly`() {
        val parseCallback: (String, LogController) -> MrzRecord = { _, _ -> mockRecord }
        skipper = MRZLineSkipper(logController, parseCallback)

        val result = skipper.tryParse("PLINE1")

        assertNotNull(result)
    }

    @Test
    fun `tryParse with empty lines should filter them out`() {
        val parseCallback: (String, LogController) -> MrzRecord = { mrzString, _ ->
            assertFalse("Empty lines should be filtered", mrzString.contains("\n\n"))
            mockRecord
        }
        skipper = MRZLineSkipper(logController, parseCallback)

        val result = skipper.tryParse("PLINE1\n\nLINE21\n")

        assertNotNull(result)
    }

    @Test
    fun `tryParse returns null when all combinations fail`() {
        skipper = MRZLineSkipper(logController, alwaysInvalidMRZCallback)

        val result = skipper.tryParse("INVALID1\nINVALID2\nINVALID3")

        assertNull(result)
    }

    @Test
    fun `checkPassportRequirements returns false and skips when second line does not end with digit`() {
        skipper = MRZLineSkipper(logController, alwaysInvalidMRZCallback)

        // First line starts with P but NO 2-line combinations end with digit
        // checkPassportRequirements should reject all 2-line combinations
        val result = skipper.tryParse("PLINE1\nLINE\nLINE")

        assertNull(result)
        // Should try only 3-line window, but all 2-line combinations fail checkPassportRequirements
        assertEquals("Expected only 1 call", 1, callCount)
    }

    @Test
    fun `tryParseLines when parseCallback matched MrzRecord returns that value`() {
        val parseCallback: (String, LogController) -> MrzRecord = { mrzString, _ ->
            mockRecord
        }
        skipper = MRZLineSkipper(logController, parseCallback)

        val result = skipper.tryParse("PLINE1\nLINE21")

        assertEquals(mockRecord,result)
    }

    @Test
    fun `tryParseLines accepts valid checksums when all individual checksums are valid`() {
        val parseCallback: (String, LogController) -> MrzRecord = { _, _ ->
            mockRecord.apply {
                whenever(validDateOfBirth).thenReturn(true)
                whenever(validDocumentNumber).thenReturn(true)
                whenever(validExpirationDate).thenReturn(true)
                whenever(validComposite).thenReturn(false)
            }
        }
        skipper = MRZLineSkipper(logController, parseCallback)

        val result = skipper.tryParse("PLINE1\nLINE21")

        assertNotNull(result)
    }

    @Test
    fun `tryParseLines rejects when all checksums are invalid`() {
        val invalidRecord: MrzRecord = mock()
        whenever(invalidRecord.validDateOfBirth).thenReturn(false)
        whenever(invalidRecord.validDocumentNumber).thenReturn(false)
        whenever(invalidRecord.validExpirationDate).thenReturn(false)
        whenever(invalidRecord.validComposite).thenReturn(false)

        val parseCallback: (String, LogController) -> MrzRecord = { _, _ ->
            invalidRecord
        }
        skipper = MRZLineSkipper(logController, parseCallback)

        val result = skipper.tryParse("PLINE1\nLINE21")

        assertNull(result)
    }

    @Test
    fun `tryParse handles 4 lines by trying 3-line and 2-line windows`() {
        var attempts = mutableListOf<String>()
        val parseCallback: (String, LogController) -> MrzRecord = { mrzString, _ ->
            attempts.add(mrzString)
            throw IllegalArgumentException("Invalid MRZ")
        }
        skipper = MRZLineSkipper(logController, parseCallback)

        // Use lines that start with P and end with digits to pass passport validation
        skipper.tryParse("PLINE1\nLINE21\nPLINE3\nLINE41")

        // Check that both 3-line and 2-line combinations were attempted
        val threeLineAttempts = attempts.count { it.split('\n').size == 3 }
        val twoLineAttempts = attempts.count { it.split('\n').size == 2 }
        assertTrue("Expected at least one 3-line attempt, got $threeLineAttempts", threeLineAttempts > 0)
        assertTrue("Expected at least one 2-line attempt, got $twoLineAttempts. Attempts: ${attempts.map { it.split('\n').size }}", twoLineAttempts > 0)
        assertTrue("Expected multiple attempts", attempts.size > 4)
    }

    @Test
    fun `tryParse returns first valid combination when multiple combinations exist`() {
        var callCount = 0
        val parseCallback: (String, LogController) -> MrzRecord = { mrzString, _ ->
            callCount++
            // Only second combination is valid
            if (callCount == 2) {
                mockRecord
            } else {
                throw IllegalArgumentException("Invalid MRZ")
            }
        }
        skipper = MRZLineSkipper(logController, parseCallback)

        val result = skipper.tryParse("PLINE1\nPLINE2\nPLINE31")

        assertEquals(mockRecord,result)
        assertEquals(2, callCount) // Should stop after finding first valid
    }

    @Test
    fun `tryParse with completely empty string returns null`() {
        val parseCallback: (String, LogController) -> MrzRecord = { _, _ ->
            mockRecord
        }
        skipper = MRZLineSkipper(logController, parseCallback)

        val result = skipper.tryParse("")

        assertNull(result)
    }

    @Test
    fun `tryParse with only newlines returns null`() {
        val parseCallback: (String, LogController) -> MrzRecord = { _, _ ->
            mockRecord
        }
        skipper = MRZLineSkipper(logController, parseCallback)

        val result = skipper.tryParse("\n\n\n")

        assertNull(result)
    }

}
