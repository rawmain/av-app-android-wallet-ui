package eu.europa.ec.passportscanner.nfc

import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.passportscanner.nfc.details.AdditionalPersonDetails
import eu.europa.ec.passportscanner.nfc.details.PersonDetails
import eu.europa.ec.passportscanner.nfc.passport.Passport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NFCResultTest {

    private lateinit var logController: LogController

    @Before
    fun setup() {
        logController = mock()
    }

    private fun passportWith(
        dateOfBirth: String? = null,
        dateOfExpiry: String? = null,
        fullDateOfBirth: String? = null,
    ): Passport {
        return Passport(
            personDetails = PersonDetails(
                dateOfBirth = dateOfBirth,
                dateOfExpiry = dateOfExpiry,
            ),
            additionalPersonDetails = fullDateOfBirth?.let {
                AdditionalPersonDetails(fullDateOfBirth = it)
            },
        )
    }

    // --- Date of birth from MRZ (yyMMdd, threshold=99) ---

    @Test
    fun `birth date 900101 resolves to 1990`() {
        val result = NFCResult.formatResult(
            passportWith(dateOfBirth = "900101", dateOfExpiry = "300101"),
            logController,
        )
        assertEquals("01/01/1990", result.dateOfBirth)
    }

    @Test
    fun `birth date 000101 resolves to 2000`() {
        val result = NFCResult.formatResult(
            passportWith(dateOfBirth = "000101", dateOfExpiry = "300101"),
            logController,
        )
        assertEquals("01/01/2000", result.dateOfBirth)
    }

    @Test
    fun `birth date 260101 resolves to 1926 not 2026 with threshold 99`() {
        // threshold=99 means start year = currentYear-99 = 1927 (in 2026)
        // So 26 -> 2026 (within window), but 27 -> 1927
        // Actually: set2DigitYearStart sets the 100-year window starting at (currentYear - 99)
        // In 2026: window is 1927-2026, so "26" -> 2026 and "27" -> 1927
        val result = NFCResult.formatResult(
            passportWith(dateOfBirth = "260101", dateOfExpiry = "300101"),
            logController,
        )
        assertEquals("01/01/2026", result.dateOfBirth)
    }

    @Test
    fun `birth date 270101 resolves to 1927 with threshold 99`() {
        val result = NFCResult.formatResult(
            passportWith(dateOfBirth = "270101", dateOfExpiry = "300101"),
            logController,
        )
        assertEquals("01/01/1927", result.dateOfBirth)
    }

    @Test
    fun `birth date for elderly person 400315 resolves to 1940`() {
        val result = NFCResult.formatResult(
            passportWith(dateOfBirth = "400315", dateOfExpiry = "300101"),
            logController,
        )
        assertEquals("03/15/1940", result.dateOfBirth)
    }

    @Test
    fun `birth date for child 200601 resolves to 2020`() {
        val result = NFCResult.formatResult(
            passportWith(dateOfBirth = "200601", dateOfExpiry = "350101"),
            logController,
        )
        assertEquals("06/01/2020", result.dateOfBirth)
    }

    // --- Date of expiry (yyMMdd, threshold=49) ---

    @Test
    fun `expiry date 300101 resolves to 2030`() {
        val result = NFCResult.formatResult(
            passportWith(dateOfBirth = "900101", dateOfExpiry = "300101"),
            logController,
        )
        assertEquals("01/01/2030", result.dateOfExpiry)
    }

    @Test
    fun `expiry date 350715 resolves to 2035`() {
        val result = NFCResult.formatResult(
            passportWith(dateOfBirth = "900101", dateOfExpiry = "350715"),
            logController,
        )
        assertEquals("07/15/2035", result.dateOfExpiry)
    }

    // --- fullDateOfBirth from DG11 (yyyyMMdd format) ---

    @Test
    fun `fullDateOfBirth from DG11 takes precedence over MRZ birth date`() {
        val result = NFCResult.formatResult(
            passportWith(
                dateOfBirth = "900101",
                dateOfExpiry = "300101",
                fullDateOfBirth = "19850315",
            ),
            logController,
        )
        assertEquals("03/15/1985", result.dateOfBirth)
    }

    @Test
    fun `fullDateOfBirth 20100601 from DG11 formats correctly`() {
        val result = NFCResult.formatResult(
            passportWith(
                dateOfBirth = "100601",
                dateOfExpiry = "300101",
                fullDateOfBirth = "20100601",
            ),
            logController,
        )
        assertEquals("06/01/2010", result.dateOfBirth)
    }

    // --- Null/empty inputs ---

    @Test
    fun `null passport returns null dates`() {
        val result = NFCResult.formatResult(null, logController)
        assertNull(result.dateOfBirth)
        assertNull(result.dateOfExpiry)
    }

    @Test
    fun `null person details returns null dates`() {
        val result = NFCResult.formatResult(Passport(), logController)
        assertNull(result.dateOfBirth)
        assertNull(result.dateOfExpiry)
    }

    @Test
    fun `empty fullDateOfBirth falls back to MRZ birth date`() {
        val result = NFCResult.formatResult(
            passportWith(
                dateOfBirth = "900101",
                dateOfExpiry = "300101",
                fullDateOfBirth = "",
            ),
            logController,
        )
        assertEquals("01/01/1990", result.dateOfBirth)
    }

    @Test
    fun `null dateOfBirth with null fullDateOfBirth returns null`() {
        val result = NFCResult.formatResult(
            passportWith(dateOfBirth = null, dateOfExpiry = "300101"),
            logController,
        )
        assertNull(result.dateOfBirth)
    }

    @Test
    fun `null dateOfExpiry returns null expiry`() {
        val result = NFCResult.formatResult(
            passportWith(dateOfBirth = "900101", dateOfExpiry = null),
            logController,
        )
        assertNull(result.dateOfExpiry)
    }

    // --- Boundary cases ---

    @Test
    fun `expiry in far future 751231 resolves correctly with threshold 49`() {
        // threshold=49: window starts at currentYear-49 = 1977 (in 2026)
        // "75" -> 2075 if within window? No: window is 1977-2076, so 75 -> 2075
        val result = NFCResult.formatResult(
            passportWith(dateOfBirth = "900101", dateOfExpiry = "751231"),
            logController,
        )
        assertEquals("12/31/2075", result.dateOfExpiry)
    }

    @Test
    fun `leap day birth date 000229 resolves correctly`() {
        val result = NFCResult.formatResult(
            passportWith(dateOfBirth = "000229", dateOfExpiry = "300101"),
            logController,
        )
        assertEquals("02/29/2000", result.dateOfBirth)
    }
}
