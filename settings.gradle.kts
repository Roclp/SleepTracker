pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        maven("https://jitpack.io")
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { setUrl ("https://jitpack.io")}
//        maven {
//            url = uri("https://mvn.0110.be/#/releases")
//        }
//        maven("https://jitpack.io")
//
        maven {
            name = "TarsosDSP repository"
            url = uri("https://mvn.0110.be/releases")
        }
        maven {
            url = uri("https://jitpack.io")
        }
        maven ("https://mvn.0110.be/releases")
//        maven (
//            "https://mvn.0110.be/releases"
//        )
    }
}

rootProject.name = "SleepTracker"
include(":app")
