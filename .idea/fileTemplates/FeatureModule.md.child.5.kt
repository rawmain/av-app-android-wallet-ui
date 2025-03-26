#set($CAPITALIZED_MODULE = $NAME.substring(0,1).toUpperCase() + $NAME.substring(1) )

package eu.europa.ec.${NAME}feature.di

import eu.europa.ec.${NAME}feature.interactor.${SCREEN_NAME}Interactor
import eu.europa.ec.${NAME}feature.interactor.${SCREEN_NAME}InteractorImpl
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module
@ComponentScan("eu.europa.ec.${NAME}feature")
class Feature${CAPITALIZED_MODULE}Module

@Factory
fun provide${SCREEN_NAME}Interactor(): ${SCREEN_NAME}Interactor = ${SCREEN_NAME}InteractorImpl()