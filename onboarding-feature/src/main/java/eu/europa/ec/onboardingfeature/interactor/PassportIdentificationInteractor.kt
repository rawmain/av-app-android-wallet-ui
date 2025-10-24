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

package eu.europa.ec.onboardingfeature.interactor

import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.periodUntil
import kotlinx.datetime.todayIn

private const val TAG = "PassportIdentificationInteractor"

sealed class PassportValidationState {
    data object Success : PassportValidationState()
    data class Failure(val error: String) : PassportValidationState()
}

interface PassportIdentificationInteractor {
    fun validatePassport(dateOfBirth: String, expiryDate: String): PassportValidationState
}

class PassportIdentificationInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val logController: LogController,
    private val clock: Clock = Clock.System,
) : PassportIdentificationInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    @OptIn(FormatStringsInDatetimeFormats::class)
    override fun validatePassport(
        dateOfBirth: String,
        expiryDate: String,
    ): PassportValidationState {
        logController.d(TAG) { "Validating passport - DOB: $dateOfBirth, Expiry: $expiryDate" }

        return try {
            val today = clock.todayIn(TimeZone.currentSystemDefault())

            // Parse dates - try multiple formats
            val dateFormatter = LocalDate.Format { byUnicodePattern("MM/dd/yyyy") }
            val dobDate = LocalDate.parse(dateOfBirth, dateFormatter)
            val expiry = LocalDate.parse(expiryDate, dateFormatter)

            // Check if passport is expired
            if (expiry < today) {
                val errorMessage =
                    resourceProvider.getString(R.string.passport_validation_error_expired)
                logController.w(TAG) { "Passport validation failed: expired" }
                return PassportValidationState.Failure(errorMessage)
            }

            // Check if user is at least 18 years old
            val age = dobDate.periodUntil(today).years
            if (age < 18) {
                val errorMessage =
                    resourceProvider.getString(R.string.passport_validation_error_underage)
                logController.w(TAG) { "Passport validation failed: user is $age years old" }
                return PassportValidationState.Failure(errorMessage)
            }

            logController.i(TAG) { "Passport validation successful - age: $age, expires: $expiry" }
            PassportValidationState.Success
        } catch (e: Exception) {
            logController.e(TAG) { "Error parsing passport dates: ${e.message}" }
            PassportValidationState.Failure(genericErrorMsg)
        }
    }
}
