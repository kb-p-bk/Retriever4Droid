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
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "io.objectbox") {
                useModule("io.objectbox:objectbox-gradle-plugin:${requested.version}")
            }
        }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        // ObjectBox repository
        maven { url = uri("https://repo1.maven.org/maven2/") }
    }
}

rootProject.name = "Retriever4Droid"

// App module
include(":app")

// Core modules
include(":core:common")
include(":core:model")
include(":core:ui")

// Domain module
include(":domain")

// Data modules
include(":data:parser")
include(":data:chunking")
include(":data:embedding")
include(":data:vectorstore")
include(":data:file")
include(":data:repository")

// Feature modules
include(":feature:home")
include(":feature:ingest")
include(":feature:search")
include(":feature:library")
