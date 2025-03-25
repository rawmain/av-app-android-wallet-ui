package eu.europa.ec.${NAME}feature.interactor

interface ${SCREEN_NAME}Interactor {
   fun getNextRoute(): String
}

class ${SCREEN_NAME}InteractorImpl : ${SCREEN_NAME}Interactor {
    override fun getNextRoute() : String {
        return "nextRoute"
    }
    
}