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

<<<<<<<< HEAD:passport-scanner/src/main/java/org/jmrtd/Verdict.kt
package org.jmrtd

/**
 * Outcome of a feature presence check.
 *
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: 1559 $
 */
enum class Verdict {
    UNKNOWN,
    PRESENT,
    NOT_PRESENT;
}
========
package eu.europa.ec.dashboardfeature.ui.document_sign.model

import eu.europa.ec.uilogic.component.ListItemDataUi

data class DocumentSignButtonUi(
    val data: ListItemDataUi
)
>>>>>>>> ref/main:dashboard-feature/src/main/java/eu/europa/ec/dashboardfeature/ui/document_sign/model/DocumentSignButtonUi.kt
