#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != "")package ${PACKAGE_NAME};#end

#set($CAPITALIZED_MODULE = $NAME.substring(0,1).toUpperCase() + $NAME.substring(1) )
#set($CAPITALIZED_SCREEN = $SCREEN_NAME.toUpperCase())

To fully integrate a new feature module follow these steps:

1. add module to top level [settings.gradle.kts](/settings.gradle.kts)
```
include(":${NAME}-feature")
```

2. add to [LibraryPluginConfig.kt](/build-logic/convention/src/main/kotlin/project/convention/logic/config/LibraryPluginConfig.kt)
```
 ${CAPITALIZED_MODULE}Feature(":${NAME}-feature"),
```
3. add to [KoverExclusionRules.kt](/build-logic/convention/src/main/kotlin/project/convention/logic/kover/KoverExclusionRules.kt)
```kotlin
LibraryModule.${CAPITALIZED_MODULE}Feature to KoverExclusionRules.${CAPITALIZED_MODULE}Feature,
```
and further 
```kotlin
object ${CAPITALIZED_MODULE}Feature : FeatureModule {
    override val classes: List<String>
        get() = commonClasses

    override val packages: List<String>
        get() = commonPackages 
}
```

4. add module to [assembly build.gradle](/assembly-logic/build.gradle.kts)
```kotlin
api(project(LibraryModule.${CAPITALIZED_MODULE}Feature.path))
```

5. perform gradle sync

6. add to [AssemblyModule.kt](/assembly-logic/src/main/java/eu/europa/ec/assemblylogic/di/AssemblyModule.kt)
```
Feature${CAPITALIZED_MODULE}Module().module,
```

7. add nav graph to [MainActivity.kt](/assembly-logic/src/main/java/eu/europa/ec/assemblylogic/ui/MainActivity.kt)
```
feature${CAPITALIZED_MODULE}Graph(it)
```

8. add screens sealed class in [RouterContract.kt](ui-logic/src/main/java/eu/europa/ec/uilogic/navigation/RouterContract.kt)
```
sealed class ${CAPITALIZED_MODULE}Screens {
    data object ${SCREEN_NAME} : Screen(name = "$CAPITALIZED_SCREEN")
}
```

