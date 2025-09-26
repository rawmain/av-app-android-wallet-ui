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

package eu.europa.ec.uilogic.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import eu.europa.ec.resourceslogic.R

/**
 * Data class to be used when we want to display an Icon.
 * @param resourceId The id of the icon. Can be null
 * @param contentDescriptionId The id of its content description.
 * @param imageVector The [ImageVector] of the icon, null by default.
 * @throws IllegalArgumentException If both [resourceId] AND [imageVector] are null.
 */
@Stable
data class IconData(
    @DrawableRes val resourceId: Int?,
    @StringRes val contentDescriptionId: Int,
    val imageVector: ImageVector? = null,
) {
    init {
        require(
            resourceId == null && imageVector != null
                    || resourceId != null && imageVector == null
                    || resourceId != null && imageVector != null
        ) {
            "An Icon should at least have a valid resourceId or a valid imageVector."
        }
    }
}

/**
 * A Singleton object responsible for providing access to all the app's Icons.
 */
object AppIcons {

    val ArrowBack: IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_arrow_back_icon,
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
    )

    val Close: IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_close_icon,
        imageVector = Icons.Filled.Close
    )

    val VerticalMore: IconData = IconData(
        resourceId = R.drawable.ic_more,
        contentDescriptionId = R.string.content_description_more_vert_icon,
        imageVector = null
    )

    val Warning: IconData = IconData(
        resourceId = R.drawable.ic_warning,
        contentDescriptionId = R.string.content_description_warning_icon,
        imageVector = null
    )

    val Error: IconData = IconData(
        resourceId = R.drawable.ic_error,
        contentDescriptionId = R.string.content_description_error_icon,
        imageVector = null
    )

    val TouchId: IconData = IconData(
        resourceId = R.drawable.ic_touch_id,
        contentDescriptionId = R.string.content_description_touch_id_icon,
        imageVector = null
    )

    val QR: IconData = IconData(
        resourceId = R.drawable.ic_qr,
        contentDescriptionId = R.string.content_description_qr_icon,
        imageVector = null
    )

    val User: IconData = IconData(
        resourceId = R.drawable.ic_user,
        contentDescriptionId = R.string.content_description_user_icon,
        imageVector = null
    )

    val Id: IconData = IconData(
        resourceId = R.drawable.ic_id,
        contentDescriptionId = R.string.content_description_id_icon,
        imageVector = null
    )

    val LogoPlain: IconData = IconData(
        resourceId = R.drawable.ic_logo_plain,
        contentDescriptionId = R.string.content_description_logo_plain_icon,
        imageVector = null
    )

    val LogoText: IconData = IconData(
        resourceId = R.drawable.ic_logo_text,
        contentDescriptionId = R.string.content_description_logo_text_icon,
        imageVector = null
    )

    val KeyboardArrowDown: IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_arrow_down_icon,
        imageVector = Icons.Default.KeyboardArrowDown
    )

    val KeyboardArrowUp: IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_arrow_up_icon,
        imageVector = Icons.Default.KeyboardArrowUp
    )

    val Add: IconData = IconData(
        resourceId = R.drawable.ic_add,
        contentDescriptionId = R.string.content_description_add_icon,
        imageVector = null
    )

    val Edit: IconData = IconData(
        resourceId = R.drawable.ic_edit,
        contentDescriptionId = R.string.content_description_edit_icon,
        imageVector = null
    )

    val Sign: IconData = IconData(
        resourceId = R.drawable.ic_sign_document,
        contentDescriptionId = R.string.content_description_edit_icon,
        imageVector = null
    )

    val QrScanner: IconData = IconData(
        resourceId = R.drawable.ic_qr_scanner,
        contentDescriptionId = R.string.content_description_qr_scanner_icon,
        imageVector = null
    )

    val Verified: IconData = IconData(
        resourceId = R.drawable.ic_verified,
        contentDescriptionId = R.string.content_description_verified_icon,
        imageVector = null
    )

    val Message: IconData = IconData(
        resourceId = R.drawable.ic_message,
        contentDescriptionId = R.string.content_description_message_icon,
        imageVector = null
    )

    val ClockTimer: IconData = IconData(
        resourceId = R.drawable.ic_clock_timer,
        contentDescriptionId = R.string.content_description_clock_timer_icon,
        imageVector = null
    )

    val OpenNew: IconData = IconData(
        resourceId = R.drawable.ic_open_new,
        contentDescriptionId = R.string.content_description_open_new_icon,
        imageVector = null
    )

    val KeyboardArrowRight: IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_arrow_right_icon,
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight
    )

    val HandleBar: IconData = IconData(
        resourceId = R.drawable.ic_handle_bar,
        contentDescriptionId = R.string.content_description_handle_bar_icon,
        imageVector = null
    )

    val Search: IconData = IconData(
        resourceId = R.drawable.ic_search,
        contentDescriptionId = R.string.content_description_search_icon,
        imageVector = null
    )

    val PresentDocumentInPerson: IconData = IconData(
        resourceId = R.drawable.ic_present_document_same_device,
        contentDescriptionId = R.string.content_description_present_document_same_device_icon,
        imageVector = null
    )

    val AddDocumentFromQr: IconData = IconData(
        resourceId = R.drawable.ic_add_document_from_qr,
        contentDescriptionId = R.string.content_description_add_document_from_qr_icon,
        imageVector = null
    )

    val Success: IconData = IconData(
        resourceId = R.drawable.ic_success,
        contentDescriptionId = R.string.content_description_success_icon,
        imageVector = null
    )

    val Documents: IconData = IconData(
        resourceId = R.drawable.ic_documents,
        contentDescriptionId = R.string.content_description_documents_icon,
        imageVector = null
    )

    val Filters: IconData = IconData(
        resourceId = R.drawable.ic_filters,
        contentDescriptionId = R.string.content_description_filters_icon,
        imageVector = null
    )

    val InProgress: IconData = IconData(
        resourceId = R.drawable.ic_in_progress,
        contentDescriptionId = R.string.content_description_in_progress_icon,
        imageVector = null
    )

    val WalletActivated: IconData = IconData(
        resourceId = R.drawable.ic_wallet_activated,
        contentDescriptionId = R.string.content_description_wallet_activated_icon,
        imageVector = null
    )

    val WalletSecured: IconData = IconData(
        resourceId = R.drawable.ic_wallet_secured,
        contentDescriptionId = R.string.content_description_wallet_secured_icon,
        imageVector = null
    )

    val Info: IconData = IconData(
        resourceId = R.drawable.ic_info,
        contentDescriptionId = R.string.content_description_info_icon,
        imageVector = null
    )


    val Check: IconData = IconData(
        resourceId = R.drawable.ic_check,
        contentDescriptionId = R.string.content_description_check_icon,
        imageVector = null
    )

    val Settings: IconData = IconData(
        resourceId = R.drawable.ic_settings,
        contentDescriptionId = R.string.content_description_settings_icon,
        imageVector = null
    )

    val EuFlag: IconData = IconData(
        resourceId = R.drawable.ic_eu_flag,
        contentDescriptionId = R.string.content_description_eu_flag_icon,
        imageVector = null
    )

    val Over18: IconData = IconData(
        resourceId = R.drawable.ic_over_18,
        contentDescriptionId = R.string.content_description_over_18_icon,
        imageVector = null
    )

    val NationalEID: IconData = IconData(
        resourceId = R.drawable.ic_national_eid,
        contentDescriptionId = R.string.content_description_national_eid_icon,
        imageVector = null
    )

    val EuMap: IconData = IconData(
        resourceId = R.drawable.ic_eu_map,
        contentDescriptionId = R.string.content_description_eu_map_icon,
        imageVector = null
    )

    val TelekomLogo: IconData = IconData(
        resourceId = R.drawable.ic_telekom_logo,
        contentDescriptionId = R.string.content_description_telekom_logo_icon,
        imageVector = null
    )

    val ScytalesLogo: IconData = IconData(
        resourceId = R.drawable.ic_scytales_logo,
        contentDescriptionId = R.string.content_description_scytales_logo_icon,
        imageVector = null
    )

    val DateRange: IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_date_range_icon,
        imageVector = Icons.Default.DateRange
    )

    val PassportBiometrics : IconData = IconData(
        resourceId = R.drawable.img_passport_biometric,
        contentDescriptionId = R.string.passport_biometrics_content_description,
        imageVector = null
    )
}
