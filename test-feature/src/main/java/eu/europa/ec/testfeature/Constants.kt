/*
 * Copyright (c) 2023 European Commission
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

package eu.europa.ec.testfeature

import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.eudi.wallet.document.format.DocumentData
import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.format.MsoMdocData
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcData
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.multipaz.document.NameSpacedData
import org.multipaz.securearea.SecureArea
import java.net.URI
import java.time.Instant
import java.util.Locale

const val mockedGenericErrorMessage = "resourceProvider's genericErrorMessage"
const val mockedPlainFailureMessage = "failure message"

val mockedExceptionWithMessage = RuntimeException("Exception to test interactor.")
val mockedExceptionWithNoMessage = RuntimeException()

val mockedDefaultLocale: Locale = Locale.ENGLISH

const val mockedOldestDocumentCreationDate = "2000-01-25T14:25:00.073Z"
const val mockedDocumentCreationDate = "2024-01-25T14:25:00.073Z"
const val mockedDocumentValidUntilDate = "2030-05-13T14:25:00.073Z"
const val mockedOldestPidId = "000000"
const val mockedPidId = "000001"
const val mockedMdlId = "000002"
const val mockedSdJwtPidId = "000003"
const val mockedPidDocName = "EU PID"
const val mockedDocManagerId = "managerId"
const val mockedKeyAlias = "keyAlias"
const val mockedMdlDocName = "mDL"
const val mockedBookmarkId = "mockedBookmarkId"
const val mockedVerifierIsTrusted = true
const val mockedNotifyOnAuthenticationFailure = false
val mockedPidFields: Map<String, ByteArray> = mapOf(
    "family_name" to byteArrayOf(105, 65, 78, 68, 69, 82, 83, 83, 79, 78),
    "given_name" to byteArrayOf(99, 74, 65, 78),
    "birth_date" to byteArrayOf(-39, 3, -20, 106, 49, 57, 56, 53, 45, 48, 51, 45, 51, 48),
    "age_over_18" to byteArrayOf(-11),
    "age_over_15" to byteArrayOf(-11),
    "age_over_21" to byteArrayOf(-11),
    "age_over_60" to byteArrayOf(-12),
    "age_over_65" to byteArrayOf(-12),
    "age_over_68" to byteArrayOf(-12),
    "age_in_years" to byteArrayOf(24, 38),
    "age_birth_year" to byteArrayOf(25, 7, -63),
    "family_name_birth" to byteArrayOf(105, 65, 78, 68, 69, 82, 83, 83, 79, 78),
    "given_name_birth" to byteArrayOf(99, 74, 65, 78),
    "birth_place" to byteArrayOf(102, 83, 87, 69, 68, 69, 78),
    "birth_country" to byteArrayOf(98, 83, 69),
    "birth_state" to byteArrayOf(98, 83, 69),
    "birth_city" to byteArrayOf(107, 75, 65, 84, 82, 73, 78, 69, 72, 79, 76, 77),
    "resident_address" to byteArrayOf(
        111,
        70,
        79,
        82,
        84,
        85,
        78,
        65,
        71,
        65,
        84,
        65,
        78,
        32,
        49,
        53
    ),
    "resident_country" to byteArrayOf(98, 83, 69),
    "resident_state" to byteArrayOf(98, 83, 69),
    "resident_city" to byteArrayOf(107, 75, 65, 84, 82, 73, 78, 69, 72, 79, 76, 77),
    "resident_postal_code" to byteArrayOf(101, 54, 52, 49, 51, 51),
    "resident_street" to byteArrayOf(108, 70, 79, 82, 84, 85, 78, 65, 71, 65, 84, 65, 78),
    "resident_house_number" to byteArrayOf(98, 49, 50),
    "gender" to byteArrayOf(1),
    "nationality" to byteArrayOf(98, 83, 69),
    "issuance_date" to byteArrayOf(
        -64,
        116,
        50,
        48,
        48,
        57,
        45,
        48,
        49,
        45,
        48,
        49,
        84,
        48,
        48,
        58,
        48,
        48,
        58,
        48,
        48,
        90
    ),
    "expiry_date" to byteArrayOf(
        -64,
        116,
        50,
        48,
        53,
        48,
        45,
        48,
        51,
        45,
        51,
        48,
        84,
        48,
        48,
        58,
        48,
        48,
        58,
        48,
        48,
        90
    ),
    "issuing_authority" to byteArrayOf(99, 85, 84, 79),
    "document_number" to byteArrayOf(105, 49, 49, 49, 49, 49, 49, 49, 49, 52),
    "administrative_number" to byteArrayOf(106, 57, 48, 49, 48, 49, 54, 55, 52, 54, 52),
    "issuing_country" to byteArrayOf(98, 83, 69),
    "issuing_jurisdiction" to byteArrayOf(100, 83, 69, 45, 73),
)
val mockedMdlFields: Map<String, ByteArray> = mapOf(
    "family_name" to byteArrayOf(105, 65, 78, 68, 69, 82, 83, 83, 79, 78),
    "given_name" to byteArrayOf(99, 74, 65, 78),
    "birth_date" to byteArrayOf(-39, 3, -20, 106, 49, 57, 56, 53, 45, 48, 51, 45, 51, 48),
    "issue_date" to byteArrayOf(
        -64,
        116,
        50,
        48,
        48,
        57,
        45,
        48,
        49,
        45,
        48,
        49,
        84,
        48,
        48,
        58,
        48,
        48,
        58,
        48,
        48,
        90
    ),
    "expiry_date" to byteArrayOf(
        -64,
        116,
        50,
        48,
        53,
        48,
        45,
        48,
        51,
        45,
        51,
        48,
        84,
        48,
        48,
        58,
        48,
        48,
        58,
        48,
        48,
        90
    ),
    "issuing_country" to byteArrayOf(98, 83, 69),
    "issuing_authority" to byteArrayOf(99, 85, 84, 79),
    "document_number" to byteArrayOf(105, 49, 49, 49, 49, 49, 49, 49, 49, 52),
    "portrait" to byteArrayOf(98, 83, 69),
    "un_distinguishing_sign" to byteArrayOf(97, 83),
    "administrative_number" to byteArrayOf(106, 57, 48, 49, 48, 49, 54, 55, 52, 54, 52),
    "sex" to byteArrayOf(1),
    "height" to byteArrayOf(24, -76),
    "weight" to byteArrayOf(24, 91),
    "eye_colour" to byteArrayOf(101, 98, 108, 97, 99, 107),
    "hair_colour" to byteArrayOf(101, 98, 108, 97, 99, 107),
    "birth_place" to byteArrayOf(102, 83, 87, 69, 68, 69, 78),
    "resident_address" to byteArrayOf(
        111,
        70,
        79,
        82,
        84,
        85,
        78,
        65,
        71,
        65,
        84,
        65,
        78,
        32,
        49,
        53
    ),
    "portrait_capture_date" to byteArrayOf(
        -64,
        116,
        50,
        48,
        50,
        51,
        45,
        48,
        51,
        45,
        50,
        51,
        84,
        48,
        48,
        58,
        48,
        48,
        58,
        48,
        48,
        90
    ),
    "signature_usual_mark" to byteArrayOf(98, 83, 69),
    "age_in_years" to byteArrayOf(24, 38),
    "age_birth_year" to byteArrayOf(25, 7, -63),
    "issuing_jurisdiction" to byteArrayOf(100, 83, 69, 45, 73),
    "nationality" to byteArrayOf(98, 83, 69),
    "resident_city" to byteArrayOf(102, 83, 87, 69, 68, 69, 78),
    "resident_state" to byteArrayOf(98, 83, 69),
    "resident_postal_code" to byteArrayOf(101, 54, 52, 49, 51, 51),
    "resident_country" to byteArrayOf(98, 83, 69),
    "family_name_national_character" to byteArrayOf(
        105,
        65,
        78,
        68,
        69,
        82,
        83,
        83,
        79,
        78
    ),
    "given_name_national_character" to byteArrayOf(99, 74, 65, 78),
    "age_over_15" to byteArrayOf(-11),
    "age_over_18" to byteArrayOf(-11),
    "age_over_21" to byteArrayOf(-11),
    "age_over_60" to byteArrayOf(-12),
    "age_over_65" to byteArrayOf(-12),
    "age_over_68" to byteArrayOf(-12),
)

const val mockedSdJwtFullPidFields: String =
    "eyJhbGciOiAiRVMyNTYiLCAidHlwIjogImRjK3NkLWp3dCIsICJ4NWMiOiBbIk1JSUMzekNDQW9XZ0F3SUJBZ0lVZjNsb2hUbURNQW1TL1lYL3E0aHFvUnlKQjU0d0NnWUlLb1pJemowRUF3SXdYREVlTUJ3R0ExVUVBd3dWVUVsRUlFbHpjM1ZsY2lCRFFTQXRJRlZVSURBeU1TMHdLd1lEVlFRS0RDUkZWVVJKSUZkaGJHeGxkQ0JTWldabGNtVnVZMlVnU1cxd2JHVnRaVzUwWVhScGIyNHhDekFKQmdOVkJBWVRBbFZVTUI0WERUSTFNRFF4TURFME16YzFNbG9YRFRJMk1EY3dOREUwTXpjMU1Wb3dVakVVTUJJR0ExVUVBd3dMVUVsRUlFUlRJQzBnTURFeExUQXJCZ05WQkFvTUpFVlZSRWtnVjJGc2JHVjBJRkpsWm1WeVpXNWpaU0JKYlhCc1pXMWxiblJoZEdsdmJqRUxNQWtHQTFVRUJoTUNWVlF3V1RBVEJnY3Foa2pPUFFJQkJnZ3Foa2pPUFFNQkJ3TkNBQVM3V0FBV3FQemUwVXMzejhwYWp5VlBXQlJtclJiQ2k1WDJzOUd2bHliUXl0d1R1bWNabmVqOUJrTGZBZ2xsb1g1dHYrTmdXZkRmZ3QvMDZzKzV0VjRsbzRJQkxUQ0NBU2t3SHdZRFZSMGpCQmd3Rm9BVVlzZVVSeWk5RDZJV0lLZWF3a21VUlBFQjA4Y3dHd1lEVlIwUkJCUXdFb0lRYVhOemRXVnlMbVYxWkdsM0xtUmxkakFXQmdOVkhTVUJBZjhFRERBS0JnZ3JnUUlDQUFBQkFqQkRCZ05WSFI4RVBEQTZNRGlnTnFBMGhqSm9kSFJ3Y3pvdkwzQnlaWEJ5YjJRdWNHdHBMbVYxWkdsM0xtUmxkaTlqY213dmNHbGtYME5CWDFWVVh6QXlMbU55YkRBZEJnTlZIUTRFRmdRVXFsL29weGtRbFl5MGxsYVRvUGJERS9teUVjRXdEZ1lEVlIwUEFRSC9CQVFEQWdlQU1GMEdBMVVkRWdSV01GU0dVbWgwZEhCek9pOHZaMmwwYUhWaUxtTnZiUzlsZFMxa2FXZHBkR0ZzTFdsa1pXNTBhWFI1TFhkaGJHeGxkQzloY21Ob2FYUmxZM1IxY21VdFlXNWtMWEpsWm1WeVpXNWpaUzFtY21GdFpYZHZjbXN3Q2dZSUtvWkl6ajBFQXdJRFNBQXdSUUloQU5KVlNEc3FUM0lrR2NLV1dnU2V1YmtET2RpNS9VRTliMUdGL1g1ZlFSRmFBaUJwNXQ2dEhoOFh3RmhQc3R6T0hNb3B2QkQvR3dtczBSQVVnbVNuNmt1OEdnPT0iXX0.eyJfc2QiOiBbIjJZeUtSV2U3NEQzMjJVRkFHemRMaEhUXzM3eGtaYUd4UmQxMU5oc0ZZQ1EiLCAiN0ZuQi1UOXkyWEoyLVJ2bW85NzVXTWlfYUhoTUZiU1ZJWDI5YkNIa05hZyIsICI3VndBOWI3U1JONlJjaTRnNnBUTFlNU3pyY25wWkRXXzk4ZWFDQXlJanFnIiwgIjl3QWhoeEhGUjRtbGdjOWphNHlqSWlyNTVBYVVhYXR6bzc0eEh0UnBrZEkiLCAiRkNfNEdpNV83MXVVODFmLXA3UEFxYXpaOUtoY3hiMkNVclU0X29pSGs1QSIsICJMdnJpdnRmOUpzQXFzaTUwRUpEQlUtcGpacXVkVkZ6VE93RUtzT2pHQlNFIiwgIlJfaTRqNlRLcklmRzBnY2dqOHoxYTQ1T0RyZFlkZTJlQnBvZjFEN2paSUkiLCAiYThsMHhOY0lTaG1EV3RtSkVMMm80ZHJib2YzbzdZV3BRRHg0UTNPQWFWNCIsICJidVo4N0FUQU81eXdmRzM2T1ZfMEl5NEdUZ2JWSVdqOE13ajkwWFFVVGtNIiwgImNGd3NvaFcwRktvWmlpQW5oUEtrU0hBdFJoSGlGNE15LU5jTGJuNFMtZGciLCAiY2xQcGFqWnVuWWZKOE9XdnZVVkVtb0lDREJYZHRLMk9YZGpQdWdrTGdmTSIsICJoQVlQVzhNQXlDYkg3dGhJN2p6UFFEMXpVMExrWTVvVUpHY1l5d3dPTVlvIiwgImkzWHlXYmlXY2dxcFBlWFpZZDBYXzFUUi00YmJETVUtQjVJZDZrMGtJWlUiLCAibjlJQ0lPdF9UeHNlRGpvNHhCSjgxWVZlUkVqZGYwMzN6Vnk0U1N0dlJlVSIsICJvTzd3c1lxT0VaYk9RWVNQTzZXX3lRdkNOaE5RWDZQcS1CUl9IRm5OR1drIiwgInhkMEhRYVRlWHIwcERyb1pudjJWeTFlTUlkSTNDZmhUOHFEOGFRMzFBQ1EiXSwgImlzcyI6ICJodHRwczovL2lzc3Vlci5ldWRpdy5kZXYiLCAiaWF0IjogMTc0Nzg2ODQwMCwgImV4cCI6IDE3NTU2NDQ0MDAsICJ2Y3QiOiAidXJuOmV1LmV1cm9wYS5lYy5ldWRpOnBpZDoxIiwgInN0YXR1cyI6IHsiaWRlbnRpZmllcl9saXN0IjogeyJpZCI6ICIzNzc4IiwgInVyaSI6ICJodHRwczovL2lzc3Vlci5ldWRpdy5kZXYvaWRlbnRpZmllcl9saXN0L0ZDL2V1LmV1cm9wYS5lYy5ldWRpLnBpZC4xLzk5ODdkY2E1LWYzMDEtNGY3NS1hOWVmLWQ2MmIwOTVhMGVmNCJ9LCAic3RhdHVzX2xpc3QiOiB7ImlkeCI6IDM3NzgsICJ1cmkiOiAiaHR0cHM6Ly9pc3N1ZXIuZXVkaXcuZGV2L3Rva2VuX3N0YXR1c19saXN0L0ZDL2V1LmV1cm9wYS5lYy5ldWRpLnBpZC4xLzk5ODdkY2E1LWYzMDEtNGY3NS1hOWVmLWQ2MmIwOTVhMGVmNCJ9fSwgIl9zZF9hbGciOiAic2hhLTI1NiIsICJjbmYiOiB7Imp3ayI6IHsia3R5IjogIkVDIiwgImNydiI6ICJQLTI1NiIsICJ4IjogImxfeXNWcFNCOGVaekxRRURVdTMxemxXVWRwdnU2R0RuNjJPaTh0OTh5ZjQiLCAieSI6ICJhaC1ZdUlQVFAtWmFLSzJhc21YWGlXaHFJNEZQTVRDZk5kVzNyOENFNVRZIn19fQ.xU58emwc4_r5w5uQ42foWskLtrlHNLNKMb--ZZAKhDBB57c9XYIRQbk9hxNEo7TyN9lem_muzAQX4h8JqFBR9Q~WyJaNFFkTlBaOTVVVnQ0UlJkemYxUG9nIiwgImZhbWlseV9uYW1lIiwgIkFOREVSU1NPTiJd~WyJybXY5UlNmOGY2X280dFgtYnhhclF3IiwgImdpdmVuX25hbWUiLCAiSkFOIl0~WyJIXzlZcmhlU19wRW1nbl8tWTliWE53IiwgImJpcnRoX2RhdGUiLCAiMTk4NS0wMy0zMCJd~WyI0a2xZUzgxWE5YV21TU3pYUlRQUld3IiwgImxvY2FsaXR5IiwgIktBVFJJTkVIT0xNIl0~WyJvMTRlVFE1MTBvN3dvX0NZeXBrUnhRIiwgInJlZ2lvbiIsICJTRSJd~WyJhVU9JUHV0UkFUTk5OTjAxejJsVndnIiwgImNvdW50cnkiLCAiU1dFREVOIl0~WyJ1ZXk3NVdaQmFSTDZZNkIyS18zbFpRIiwgInBsYWNlX29mX2JpcnRoIiwgeyJfc2QiOiBbIjNDdXNPSmw0SmcwMmhfM2VSbW5wclRPOFlxd1VYQ3pzUS0yQTh3Ymphb2MiLCAiNzJmR1FnMlNDeXA2UDBYNWZOMTQtR21nNEpjOC1ROGtCVHY1Y2tkTFhmNCIsICJOSzZQNXJhZXhra1B0MGNhN3hXS1VhbWU5Z1dOcWItSE43Y01qak9TSXZRIl19XQ~WyJVZHlTTUc5clE2VFRNeU1nWXM1WUlRIiwgIm5hdGlvbmFsaXRpZXMiLCBbIlNFIl1d~WyI4NzVLQnE5eXRxbmNmSGk1YXVHWGJBIiwgInN0cmVldF9hZGRyZXNzIiwgIkZPUlRVTkFHQVRBTiAxNSJd~WyJJT3c2UUVUVzRVWnBjNUEtRWtrX0dBIiwgImxvY2FsaXR5IiwgIlNFIl0~WyJGdGdxVFMwMzRWaHNYRlRrckVpeEFBIiwgInJlZ2lvbiIsICJTRSJd~WyJ5cmh6dlFNMUdXblpiSVIxSkUzU2lnIiwgInBvc3RhbF9jb2RlIiwgIjY0MTMzIl0~WyJzWkEzVUt0Sy01TWZZNXhBcFJXM3hRIiwgImNvdW50cnkiLCAiU0UiXQ~WyJFa3J2RzdQRGZnYU85bnMtU05FNWlRIiwgImhvdXNlX251bWJlciIsICIxMiJd~WyJfT2dzdzlFbHhHRTdoZHlBdTM5Ri1RIiwgImFkZHJlc3MiLCB7Il9zZCI6IFsiLVlCaHNZTUViMGV0bVc2d1hzNGJCZkN4LUFjVHdkek9DWVJjMVZ3dDZzcyIsICItYlRCNzNSemI1MklpMXhsNHY1T1phbWRJWFh2UjlOM1E5R3ZzczhRMkN3IiwgIlB6YnFDRzd1NlBqSFhLU2ZDQzQ2UGxDVkdrNG1ubW8wVTRMTHRmVWdvUU0iLCAiZjlwZV9hbEM0WmxodmlSbW1CRjdLc25wbEE1czI5VHBLWXBUV0pWc0haTSIsICJnaTlpTXpQaDYxVVJ2LXV3R280N3dVTGVIMEtacFBYdFVJZGxJbnJZeDRRIiwgIm1nbHFQaFc4b05lTkV2WGF0c1BseTR4R21PZld2OUY2MDU0VndRVHpOWVUiXX1d~WyIzLWlVcTVEWnRMREwxaG5aVnNrSEp3IiwgInBlcnNvbmFsX2FkbWluaXN0cmF0aXZlX251bWJlciIsICI5MDEwMTY3NDY0Il0~WyJsUFA0clBhc0ZPem1VMXVEZ1BBSGV3IiwgInBvcnRyYWl0IiwgIl85al80QUFRU2taSlJnQUJBUUlBSlFBbEFBRF80UUJpUlhocFpnQUFUVTBBS2dBQUFBZ0FCUUVTQUFNQUFBQUJBQUVBQUFFYUFBVUFBQUFCQUFBQVNnRWJBQVVBQUFBQkFBQUFVZ0VvQUFNQUFBQUJBQU1BQUFJVEFBTUFBQUFCQUFFQUFBQUFBQUFBQUFBbEFBQUFBUUFBQUNVQUFBQUJfOXNBUXdBREFnSUNBZ0lEQWdJQ0F3TURBd1FHQkFRRUJBUUlCZ1lGQmdrSUNnb0pDQWtKQ2d3UERBb0xEZ3NKQ1EwUkRRNFBFQkFSRUFvTUVoTVNFQk1QRUJBUV84QUFDd2dCc1FGb0FRRVJBUF9FQUI0QUFRQUJCQU1CQVFBQUFBQUFBQUFBQUFBQkFnY0lDUU1FQmdvRl84UUFTUkFBQVFNREF3TUNCUUFIQkFZR0N3QUFBUUFDQXdRRkVRWUhFZ2doTVFsQkV5SlJZWEVVSXpKQ1VvR1JGV0p5c2hnek9IV0N3aFpEb2JIUjhDUWxKalExUkVhRnM4UFVfOW9BQ0FFQkFBQV9BTnFMX1pTUENsRVJFUkVSRVJFUkVSRVJFUkVSRVJFUkVSRVJFUlV2OXZ5cEhoU2lJaUlpSWlJaTRROTNfazVJX1Bzdnk3X3FpdzZWdGs5NjFMZmFHMVctbWJ6bXFxMnBaVHdzYmo5b3ZlUUdqc2U1SUgzVmlyMTZoblJwWXE2UzMxdV90aGZORTdEalNSVk5YRm43U1F4UGE0ZmdycUQxSi1pUl93Q3h2eGJqX3dEYTdnUF9BTkNfRTFmNnBQUmhwYWlmVVV1NTFWcUNwYTNJbzdSWnF0OHpfd0FPbGpaRVAtSjRWdXo2MEhTMDRmcTlHYm5menRORF93RDJMeUo5YkRhNGFpbnAyN002cGRZVzUtQlhmcDhINlVfczBfTlQ0NE03a2pfV3U4TDhtcTlidlRVZFpQSFE5UGQxbnBoSzRRU1M2ampqZTZNZUhPWUtkM0IzMWJ5Y0I3T1hZcWZXMzBULWtVWXBOaTcyYWQ3bWlxZkxlSW12WUQ1TVliRTRQeDkzTlZ3Wl9XUTZXWXFsc0FzZTRVekJISEktYUMxVXZ3Mmx6V3VNZVgxSWNYTXlXTy1YQmN4eGE0dHc1M3Z0TGVxUDBZYW5qZ2JOdW5QWTZtVnZlQzZXZXNpTEhmd3VlMk4wWV9JZXNrOUY2NzBsdUxZYWJWV2h0VFctLVdpckdZYXVobmJMRTdfaWJudjlzaGVqUkVSRVJFUkVSRVJVdjl2eXBIaFNpSWlJaUlpSWlLbF90LVZEOEtVUkVSRjFwS2hrZVh1a3dHLVNRY0Fmbk9NRHlTZlphU19VOTZ2bmIyN2lPMm0wVGRYUzZLMGhVUFktU25lVERYMXd5MTh2YnM1ckFYTmFUN2x4LWl3VTVIT2NuUDFSRVJFUkVSRVJFUkVSRTVINmxYbjZZZXAzWF9TM3VGSHJqUnRRYWlsbmFJYnBhcFhFUTNDbnlTUTdIaHdKUEYza1pPRnZxNmMtb0hSUFV0dGJRYm9hRm5kSFQxRGpUVjFIS1FacmZXTURUSkJKanNEaHpIQS1DMTdYZUNBTHJvaUlpSWlLbF90LVZEOEtVUkVSRnJkOVVQcmVPZ2JUVjlPMjFsMWxoMUxkSWZfYUM0VTctSm9xUnpEbW5hN3lKWGpISS16UGw4dUhIVDJaSFpKOEUtVlNpSWlJaUlpSWlJaUlpSXA1T0hoeF9xc3BlZ3JxNnV2U3p1ZXlTNHpTejZLMUctT212bEtDZU1CemdWVFJfRXdlUjdzSl9oQzM2VzI1MGQyb0thNTIycVpVMHRaRXllR1ZqZ1EtTjRCYTRZN0VFRUgtYTd5SWlJaUlxWC0zNVVqd3BSRVJGMUtxc2hvS2VXc3E1MnhRUXNkSkpJOTJBMWdCSkp6MndBQ1NUOUY4MWZVSHJhUGNYZkxYV3VLT3NkV1UxM3ZsWlVVMF9mOVpBSlNJM2o2RGcxdVBzcmJJaUlpSWlJaUlpSWlJaUlpcUQzZTN0Mlc1bjBqZXBpdTNKMjNyOWtOVlZqNTd0b1dKa2xybmxlQzZXMlBQRnNaOV8xVHNOX3dBTG94NEMyR29pSWlJaXBmN2ZsU1BDbEVSRVdJWHFpN25YdmJEcEp2NzlQMWJxV3IxVlhVMm5ETzEtSE1obmE5MHdiNzVkRkRJMzdDUng4Z0xRcTRueDdmUlFpSWlJaUlpSWlJaUl2XzJRPT0iXQ~WyJfR2p3dm5vV2VvV0pNbnFjT1d3ZndnIiwgImJpcnRoX2ZhbWlseV9uYW1lIiwgIkFOREVSU1NPTiJd~WyJfc3hPNDdqaU9jcTNxYWVMOElBdkVRIiwgImJpcnRoX2dpdmVuX25hbWUiLCAiSkFOIl0~WyJyQnVOVDBwbTl3LUJGSUYwWkpRbkJnIiwgImJpcnRoX2RhdGUiLCAiMTk4NS0wMy0zMCJd~WyJzdVhSSl9JLXVjbk1vVVctN1Z4RmpBIiwgImxvY2FsaXR5IiwgIktBVFJJTkVIT0xNIl0~WyJQUmJKQlZ2eWxaV2dCTndVajRYUmpRIiwgInBsYWNlX29mX2JpcnRoIiwgeyJfc2QiOiBbIlJ3WEZMOTJicFU0YmFlNmhDTGMyVFlBc2ZmaVc0U1ZCQ2tSYUtseHJtN3ciXX1d~WyJWRWpnOW1ROGV2d3VtRGE3bTc1WVB3IiwgIm5hdGlvbmFsaXRpZXMiLCBbIlNFIl1d~WyJvbTVpYUxVdzRNblZSelQ2Nk53bmtnIiwgIjE4IiwgInRydWUiXQ~WyJPUDA5OU5ZN29LcFM1R1pQRDZrU19RIiwgIjY1IiwgInVuc2V0Il0~WyJVYTA0TFJOc05DUjF6SU96VGRTaTlBIiwgImFnZV9lcXVhbF9vcl9vdmVyIiwgeyJfc2QiOiBbIi05cm16RVJJWHl5X0trQWdBbC1vMF82RGJad05lWjZ0Y3FLRzl1V0RhX0kiLCAicDBQNXROU1VxYXdQU2p4dVdmQ3paZmNaNENfYVFTV0V3TmhjT28zZ3ZkNCJdfV0~WyJXWEdxdm9zR0RuOWNaWm11Y2FSdFFBIiwgImFnZV9iaXJ0aF95ZWFyIiwgMTk4NV0~WyI3QTQtNkZHaURhRThfQ1laTEw0OERnIiwgImlzc3VpbmdfYXV0aG9yaXR5IiwgIlRlc3QgUElEIGlzc3VlciJd~WyJVU0JRX2ZhaUl1X0pkQnl3dnVZd1dnIiwgImlzc3VpbmdfY291bnRyeSIsICJGQyJd~"

const val mockedSdJwtPidBasicFields: String =
    "eyJhbGciOiAiRVMyNTYiLCAidHlwIjogImRjK3NkLWp3dCIsICJ4NWMiOiBbIk1JSUMzekNDQW9XZ0F3SUJBZ0lVZjNsb2hUbURNQW1TL1lYL3E0aHFvUnlKQjU0d0NnWUlLb1pJemowRUF3SXdYREVlTUJ3R0ExVUVBd3dWVUVsRUlFbHpjM1ZsY2lCRFFTQXRJRlZVSURBeU1TMHdLd1lEVlFRS0RDUkZWVVJKSUZkaGJHeGxkQ0JTWldabGNtVnVZMlVnU1cxd2JHVnRaVzUwWVhScGIyNHhDekFKQmdOVkJBWVRBbFZVTUI0WERUSTFNRFF4TURFME16YzFNbG9YRFRJMk1EY3dOREUwTXpjMU1Wb3dVakVVTUJJR0ExVUVBd3dMVUVsRUlFUlRJQzBnTURFeExUQXJCZ05WQkFvTUpFVlZSRWtnVjJGc2JHVjBJRkpsWm1WeVpXNWpaU0JKYlhCc1pXMWxiblJoZEdsdmJqRUxNQWtHQTFVRUJoTUNWVlF3V1RBVEJnY3Foa2pPUFFJQkJnZ3Foa2pPUFFNQkJ3TkNBQVM3V0FBV3FQemUwVXMzejhwYWp5VlBXQlJtclJiQ2k1WDJzOUd2bHliUXl0d1R1bWNabmVqOUJrTGZBZ2xsb1g1dHYrTmdXZkRmZ3QvMDZzKzV0VjRsbzRJQkxUQ0NBU2t3SHdZRFZSMGpCQmd3Rm9BVVlzZVVSeWk5RDZJV0lLZWF3a21VUlBFQjA4Y3dHd1lEVlIwUkJCUXdFb0lRYVhOemRXVnlMbVYxWkdsM0xtUmxkakFXQmdOVkhTVUJBZjhFRERBS0JnZ3JnUUlDQUFBQkFqQkRCZ05WSFI4RVBEQTZNRGlnTnFBMGhqSm9kSFJ3Y3pvdkwzQnlaWEJ5YjJRdWNHdHBMbVYxWkdsM0xtUmxkaTlqY213dmNHbGtYME5CWDFWVVh6QXlMbU55YkRBZEJnTlZIUTRFRmdRVXFsL29weGtRbFl5MGxsYVRvUGJERS9teUVjRXdEZ1lEVlIwUEFRSC9CQVFEQWdlQU1GMEdBMVVkRWdSV01GU0dVbWgwZEhCek9pOHZaMmwwYUhWaUxtTnZiUzlsZFMxa2FXZHBkR0ZzTFdsa1pXNTBhWFI1TFhkaGJHeGxkQzloY21Ob2FYUmxZM1IxY21VdFlXNWtMWEpsWm1WeVpXNWpaUzFtY21GdFpYZHZjbXN3Q2dZSUtvWkl6ajBFQXdJRFNBQXdSUUloQU5KVlNEc3FUM0lrR2NLV1dnU2V1YmtET2RpNS9VRTliMUdGL1g1ZlFSRmFBaUJwNXQ2dEhoOFh3RmhQc3R6T0hNb3B2QkQvR3dtczBSQVVnbVNuNmt1OEdnPT0iXX0.eyJfc2QiOiBbIkJUd09DRlNTSW1VQkdnRVkyQTQxSnlFN08tN3ZHbmxsWmNYdzhWd2t6aEkiLCAiRHdFSktNTkllaDkteW1GMFNsaHIwSzVRNkV5ZjJMVXo5WXlJQ3l6RnFLUSIsICJFWnhPQkx0UUdsSjYtU3lXTEFObXJlQVVST3NxU1lOWW9yRUZwMG40MHhzIiwgIkY5ZWs5eU5QVDI4MlppX1J6N0pXNzZ4cThpMHJlTDdvN0ozcDMzRjdqSlkiLCAiU3c5VEF2S1pHQXVYbnJDa0Jqbkx4YU5wdUszNkJWVUlzaDhoVGZVcWVfWSIsICJYZDNHTm9oWXNORkREN2JYN3kzWks2d3RzcU85OURSYWMyckxqRnBuT3RFIiwgIlhxVk9kbzRzQjZSZlo3aFlmdnhBZW5ZSjV5dE1MWERfUlhCVzZ6SGJrNGMiLCAiWUtOWXY1MnZaUHNnM0pFcTRNU2c2X0FFQkF0dnVaT2trMXZ3UWZPam5vdyIsICJnR3pVOExNYW1CTTF4X3pEOFBOUUk4anpZVU1uaHJUa09PQU1RdVdlcVVBIl0sICJpc3MiOiAiaHR0cHM6Ly9pc3N1ZXIuZXVkaXcuZGV2IiwgImlhdCI6IDE3NDc5NTQ4MDAsICJleHAiOiAxNzU1NzMwODAwLCAidmN0IjogInVybjpldS5ldXJvcGEuZWMuZXVkaTpwaWQ6MSIsICJzdGF0dXMiOiB7ImlkZW50aWZpZXJfbGlzdCI6IHsiaWQiOiAiNzgwMyIsICJ1cmkiOiAiaHR0cHM6Ly9pc3N1ZXIuZXVkaXcuZGV2L2lkZW50aWZpZXJfbGlzdC9GQy9ldS5ldXJvcGEuZWMuZXVkaS5waWQuMS85OTg3ZGNhNS1mMzAxLTRmNzUtYTllZi1kNjJiMDk1YTBlZjQifSwgInN0YXR1c19saXN0IjogeyJpZHgiOiA3ODAzLCAidXJpIjogImh0dHBzOi8vaXNzdWVyLmV1ZGl3LmRldi90b2tlbl9zdGF0dXNfbGlzdC9GQy9ldS5ldXJvcGEuZWMuZXVkaS5waWQuMS85OTg3ZGNhNS1mMzAxLTRmNzUtYTllZi1kNjJiMDk1YTBlZjQifX0sICJfc2RfYWxnIjogInNoYS0yNTYiLCAiY25mIjogeyJqd2siOiB7Imt0eSI6ICJFQyIsICJjcnYiOiAiUC0yNTYiLCAieCI6ICJGOWdpRy02Wmh6WV80Rm13WGtqX191UzBLOTZhMzdURDJxNjVQelAtY3M0IiwgInkiOiAiMHd0Y19IdTc1QnI4XzZabnctcHEtSUdlUjhpVkdFOUdJVElHdFBQRmRMayJ9fX0.2Zuy9m9Y9qj13BID7U8er--byhTta5m7xxuSv1i4udn7ydMX8Zb4uKBCutzQBe-1b7D5_qurPbYTJbCRQVqT1A~WyJRT0lhYmgtV2tlSU1kZVlCdFhvUEtBIiwgImZhbWlseV9uYW1lIiwgIkFOREVSU1NPTiJd~WyIyTHlJcjJWeWFKSFRsdzlRU3cwd3VBIiwgImdpdmVuX25hbWUiLCAiSkFOIl0~WyJyQnVOVDBwbTl3LUJGSUYwWkpRbkJnIiwgImJpcnRoX2RhdGUiLCAiMTk4NS0wMy0zMCJd~WyJzdVhSSl9JLXVjbk1vVVctN1Z4RmpBIiwgImxvY2FsaXR5IiwgIktBVFJJTkVIT0xNIl0~WyJQUmJKQlZ2eWxaV2dCTndVajRYUmpRIiwgInBsYWNlX29mX2JpcnRoIiwgeyJfc2QiOiBbIlJ3WEZMOTJicFU0YmFlNmhDTGMyVFlBc2ZmaVc0U1ZCQ2tSYUtseHJtN3ciXX1d~WyJWRWpnOW1ROGV2d3VtRGE3bTc1WVB3IiwgIm5hdGlvbmFsaXRpZXMiLCBbIlNFIl1d~WyJvbTVpYUxVdzRNblZSelQ2Nk53bmtnIiwgIjE4IiwgInRydWUiXQ~WyJPUDA5OU5ZN29LcFM1R1pQRDZrU19RIiwgIjY1IiwgInVuc2V0Il0~WyJVYTA0TFJOc05DUjF6SU96VGRTaTlBIiwgImFnZV9lcXVhbF9vcl9vdmVyIiwgeyJfc2QiOiBbIi05cm16RVJJWHl5X0trQWdBbC1vMF82RGJad05lWjZ0Y3FLRzl1V0RhX0kiLCAicDBQNXROU1VxYXdQU2p4dVdmQ3paZmNaNENfYVFTV0V3TmhjT28zZ3ZkNCJdfV0~WyJXWEdxdm9zR0RuOWNaWm11Y2FSdFFBIiwgImFnZV9iaXJ0aF95ZWFyIiwgMTk4NV0~WyI3QTQtNkZHaURhRThfQ1laTEw0OERnIiwgImlzc3VpbmdfYXV0aG9yaXR5IiwgIlRlc3QgUElEIGlzc3VlciJd~WyJVU0JRX2ZhaUl1X0pkQnl3dnVZd1dnIiwgImlzc3VpbmdfY291bnRyeSIsICJGQyJd~"

val mockedPidBasicFields: Map<String, ByteArray> = mapOf(
    "family_name" to byteArrayOf(105, 65, 78, 68, 69, 82, 83, 83, 79, 78),
    "given_name" to byteArrayOf(99, 74, 65, 78),
    "age_over_18" to byteArrayOf(-11),
    "age_over_65" to byteArrayOf(-12),
    "age_birth_year" to byteArrayOf(25, 7, -63),
    "birth_city" to byteArrayOf(107, 75, 65, 84, 82, 73, 78, 69, 72, 79, 76, 77),
    "gender" to byteArrayOf(1),
    "expiry_date" to byteArrayOf(
        -64,
        116,
        50,
        48,
        53,
        48,
        45,
        48,
        51,
        45,
        51,
        48,
        84,
        48,
        48,
        58,
        48,
        48,
        58,
        48,
        48,
        90
    ),
)

val mockedMdlBasicFields: Map<String, ByteArray> = mapOf(
    "family_name" to byteArrayOf(105, 65, 78, 68, 69, 82, 83, 83, 79, 78),
    "given_name" to byteArrayOf(99, 74, 65, 78),
    "birth_place" to byteArrayOf(102, 83, 87, 69, 68, 69, 78),
    "expiry_date" to byteArrayOf(
        -64,
        116,
        50,
        48,
        53,
        48,
        45,
        48,
        51,
        45,
        51,
        48,
        84,
        48,
        48,
        58,
        48,
        48,
        58,
        48,
        48,
        90
    ),
    "portrait" to byteArrayOf(98, 83, 69),

    "signature_usual_mark" to byteArrayOf(98, 83, 69),
    "sex" to byteArrayOf(1),
)

val mockedAgeVerificationBasicFields: Map<String, ByteArray> = mapOf(
    "age_over_18" to byteArrayOf(-11),
    "family_name" to byteArrayOf(105, 65, 78, 68, 69, 82, 83, 83, 79, 78),
    "given_name" to byteArrayOf(99, 74, 65, 78),
    "birth_date" to byteArrayOf(-39, 3, -20, 106, 49, 57, 56, 53, 45, 48, 51, 45, 51, 48),
)

const val mockedPidDocType = "eu.europa.ec.eudi.pid.1"
const val mockedPidNameSpace = "eu.europa.ec.eudi.pid.1"
const val mockedAgeVerificationNameSpaceAndType = "eu.europa.ec.agev10n"
const val mockedMdlDocType = "org.iso.18013.5.1.mDL"
const val mockedMdlNameSpace = "org.iso.18013.5.1"
const val mockedDocDisplayLogo = "https://examplestate.com/public/pid.png"
const val mockedIssuerLogo = "https://issuer.eudiw.dev/ic-logo.png"
const val mockedIssuerIdentifier = "https://issuer.eudiw.dev"

fun createMockedNamespaceData(
    documentNamespace: String,
    nameSpacedData: Map<String, ByteArray>,
): NameSpacedData {
    val builder = NameSpacedData.Builder()
    nameSpacedData.forEach {
        builder.putEntry(documentNamespace, it.key, it.value)
    }
    return builder.build()
}

fun createMockedSecureArea(): SecureArea {
    return mock<SecureArea> {
        on { this.identifier } doReturn "mockedSecureAreaId"
        on { this.displayName } doReturn "mockedSecureAreaName"
        on { this.supportedAlgorithms } doReturn listOf()
    }
}

fun createMockedIssuedDocument(
    id: String = mockedPidId,
    name: String = mockedPidDocName,
    documentManagerId: String = mockedDocManagerId,
    isCertified: Boolean = false,
    keyAlias: String = mockedKeyAlias,
    secureArea: SecureArea = createMockedSecureArea(),
    createdAt: Instant = Instant.parse(mockedDocumentCreationDate),
    issuedAt: Instant = Instant.parse(mockedDocumentCreationDate),
    validFrom: Instant = Instant.now(),
    validUntil: Instant = Instant.parse(mockedDocumentValidUntilDate),
    issuerProvidedData: ByteArray = byteArrayOf(),
    data: DocumentData = MsoMdocData(
        format = MsoMdocFormat(mockedPidNameSpace),
        issuerMetadata = null,
        nameSpacedData = createMockedNamespaceData(
            mockedPidNameSpace,
            mockedPidBasicFields
        )
    ),
    credentialsCount: Int = 1,
): IssuedDocument {
    return mock<IssuedDocument> {
        on { this.id } doReturn id
        on { this.name } doReturn name
        on { this.format } doReturn data.format
        on { this.documentManagerId } doReturn documentManagerId
        on { this.isCertified } doReturn isCertified
        on { this.keyAlias } doReturn keyAlias
        on { this.secureArea } doReturn secureArea
        on { this.createdAt } doReturn createdAt
        on { this.issuedAt } doReturn issuedAt
        on { this.validFrom } doReturn validFrom
        on { this.validUntil } doReturn validUntil
        on { this.issuerProvidedData } doReturn issuerProvidedData
        on { this.data } doReturn data
        onBlocking { credentialsCount() } doReturn credentialsCount
    }
}

fun createMockedSdJwtFullPid(): IssuedDocument {
    return createMockedIssuedDocument(
        id = mockedSdJwtPidId,
        documentManagerId = "fabulas",
        keyAlias = "massa",
        data = SdJwtVcData(
            format = SdJwtVcFormat(mockedPidNameSpace),
            issuerMetadata = null,
            sdJwtVc = mockedSdJwtFullPidFields,
        )
    )
}

fun createMockedUnsignedDocument(
    id: String = mockedPidId,
    name: String = mockedPidDocName,
    createdAt: Instant = Instant.parse(mockedDocumentCreationDate),
    format: DocumentFormat = MsoMdocFormat(mockedPidDocType),
    documentManagerId: String = mockedDocManagerId,
    isCertified: Boolean = false,
    keyAlias: String = mockedKeyAlias,
    secureArea: SecureArea = createMockedSecureArea(),
    issuerMetadata: IssuerMetadata? = null,
): UnsignedDocument {
    return mock<UnsignedDocument> {
        on { this.id } doReturn id
        on { this.name } doReturn name
        on { this.createdAt } doReturn createdAt
        on { this.format } doReturn format
        on { this.documentManagerId } doReturn documentManagerId
        on { this.isCertified } doReturn isCertified
        on { this.keyAlias } doReturn keyAlias
        on { this.secureArea } doReturn secureArea
        on { this.issuerMetadata } doReturn issuerMetadata
    }
}

fun createMockedMainPid(): IssuedDocument = createMockedIssuedDocument()

fun createMockedPidWithBasicFields(
    data: DocumentData = MsoMdocData(
        format = MsoMdocFormat(mockedPidNameSpace),
        issuerMetadata = null,
        nameSpacedData = createMockedNamespaceData(
            mockedPidNameSpace,
            mockedPidBasicFields
        )
    ),
): IssuedDocument = createMockedIssuedDocument(
    documentManagerId = "fabulas",
    keyAlias = "massa",
    data = data
)

fun createMockedDocumentMetadata(): IssuerMetadata = IssuerMetadata(
    documentConfigurationIdentifier = mockedPidDocType,
    display = listOf(
        IssuerMetadata.Display(
            name = mockedPidDocName,
            logo = IssuerMetadata.Logo(
                uri = URI.create(mockedDocDisplayLogo)
            ),
            locale = Locale.ENGLISH
        )
    ),
    claims = emptyList(),
    credentialIssuerIdentifier = mockedIssuerIdentifier,
    issuerDisplay = listOf(
        IssuerMetadata.IssuerDisplay(
            name = "EUDIW Issuer",
            logo = IssuerMetadata.Logo(
                uri = URI.create(mockedIssuerLogo)
            ),
            locale = Locale.ENGLISH
        )
    )
)

fun createMockedPidWithBasicFieldsAndMetadata(): IssuedDocument = createMockedIssuedDocument(
    documentManagerId = "fabulas",
    keyAlias = "massa",
    data = MsoMdocData(
        format = MsoMdocFormat(mockedPidNameSpace),
        issuerMetadata = createMockedDocumentMetadata(),
        nameSpacedData = createMockedNamespaceData(
            mockedPidNameSpace,
            mockedPidBasicFields
        )
    )
)

fun createMockedSdJwtPidWithBasicFields(): IssuedDocument = createMockedIssuedDocument(
    id = mockedSdJwtPidId,
    documentManagerId = "fabulas",
    keyAlias = "massa",
    data = SdJwtVcData(
        format = SdJwtVcFormat(mockedPidNameSpace),
        issuerMetadata = null,
        sdJwtVc = mockedSdJwtPidBasicFields,
    )
)

fun createMockedOldestPidWithBasicFields(): IssuedDocument = createMockedIssuedDocument(
    id = mockedOldestPidId,
    documentManagerId = "fabulas",
    keyAlias = "massa",
    createdAt = Instant.parse(mockedOldestDocumentCreationDate),
    data = MsoMdocData(
        format = MsoMdocFormat(mockedPidNameSpace),
        issuerMetadata = null,
        nameSpacedData = createMockedNamespaceData(
            mockedPidNameSpace,
            mockedPidBasicFields
        )
    )
)

fun createMockedEmptyPid(): IssuedDocument = createMockedIssuedDocument(
    documentManagerId = "fabulas",
    keyAlias = "massa",
    data = MsoMdocData(
        format = MsoMdocFormat(mockedPidNameSpace),
        issuerMetadata = null,
        nameSpacedData = createMockedNamespaceData(
            mockedPidNameSpace,
            emptyMap()
        )
    )
)

fun createMockedFullMdl(): IssuedDocument = createMockedIssuedDocument(
    id = mockedMdlId,
    name = mockedMdlDocName,
    documentManagerId = "fabulas",
    keyAlias = "massa",
    data = MsoMdocData(
        format = MsoMdocFormat(mockedMdlDocType),
        issuerMetadata = null,
        nameSpacedData = createMockedNamespaceData(
            mockedMdlNameSpace,
            mockedMdlFields
        )
    )
)

fun createMockedMdlWithBasicFields(): IssuedDocument = createMockedIssuedDocument(
    id = mockedMdlId,
    name = mockedMdlDocName,
    documentManagerId = "fabulas",
    keyAlias = "massa",
    data = MsoMdocData(
        format = MsoMdocFormat(mockedMdlDocType),
        issuerMetadata = null,
        nameSpacedData = createMockedNamespaceData(
            mockedMdlNameSpace,
            mockedMdlBasicFields
        )
    )
)

fun createMockedMdlWithNoExpirationDate(): IssuedDocument = createMockedIssuedDocument(
    id = mockedMdlId,
    name = mockedMdlDocName,
    documentManagerId = "fabulas",
    keyAlias = "massa",
    data = MsoMdocData(
        format = MsoMdocFormat(mockedMdlDocType),
        issuerMetadata = null,
        nameSpacedData = createMockedNamespaceData(
            mockedMdlNameSpace,
            mockedMdlFields.minus("expiry_date")
        )
    )
)

fun createMockedMdlWithNoUserNameAndNoUserImage(): IssuedDocument = createMockedIssuedDocument(
    id = mockedMdlId,
    name = mockedMdlDocName,
    documentManagerId = "fabulas",
    keyAlias = "massa",
    data = MsoMdocData(
        format = MsoMdocFormat(mockedMdlDocType),
        issuerMetadata = null,
        nameSpacedData = createMockedNamespaceData(
            mockedMdlNameSpace,
            mockedMdlFields
                .minus("given_name")
                .minus("portrait")
        )
    )
)

fun createMockedAgeVerificationDocument(): IssuedDocument = createMockedIssuedDocument(
    id = mockedMdlId,
    name = mockedMdlDocName,
    documentManagerId = "fabulas",
    keyAlias = "massa",
    data = MsoMdocData(
        format = MsoMdocFormat(mockedAgeVerificationNameSpaceAndType),
        issuerMetadata = null,
        nameSpacedData = createMockedNamespaceData(
            mockedAgeVerificationNameSpaceAndType,
            mockedAgeVerificationBasicFields
        )
    )
)

fun createMockedFullDocuments(): List<IssuedDocument> = listOf(
    createMockedMainPid(), createMockedFullMdl()
)