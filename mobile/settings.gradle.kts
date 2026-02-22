import java.util.Properties

val secretsProperties = Properties().apply {
    val secretsFile = file("secrets.properties")
    if (secretsFile.exists()) {
        secretsFile.inputStream().use { load(it) }
    }
}

val mapboxDownloadsToken = (
    secretsProperties.getProperty("MAPBOX_DOWNLOADS_TOKEN")
        ?: secretsProperties.getProperty("MAPBOX_SECRET_KEY")
        ?: System.getenv("MAPBOX_DOWNLOADS_TOKEN")
).orEmpty().trim()

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
        google()
        mavenCentral()
        maven("https://api.mapbox.com/downloads/v2/releases/maven") {
            credentials {
                username = "mapbox"
                password = mapboxDownloadsToken
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
        maven("https://jitpack.io")
    }
}

rootProject.name = "Mobile"
include(":app")
