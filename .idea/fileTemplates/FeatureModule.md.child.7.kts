#set($CAPITALIZED_MODULE = $NAME.substring(0,1).toUpperCase() + $NAME.substring(1) )
import project.convention.logic.config.LibraryModule
import project.convention.logic.kover.KoverExclusionRules
import project.convention.logic.kover.excludeFromKoverReport

plugins {
    id("project.android.feature")
}

android {
    namespace = "eu.europa.ec.${NAME}feature"
}

moduleConfig {
    module = LibraryModule.${CAPITALIZED_MODULE}Feature
}

excludeFromKoverReport(
    excludedClasses = KoverExclusionRules.${CAPITALIZED_MODULE}Feature.classes,
    excludedPackages = KoverExclusionRules.${CAPITALIZED_MODULE}Feature.packages,
)