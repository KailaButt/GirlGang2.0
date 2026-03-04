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
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // ✅ Google Maven (explicit)
        maven("https://dl.google.com/dl/android/maven2/")
        maven("https://maven.google.com")

        // ✅ Standard repos
        google()
        mavenCentral()
    }
}

rootProject.name = "ConsoliCalm"
include(":app")
