rootProject.name = "androidkrdbot"

pluginManagement {
    repositories {
        maven( "https://jitpack.io" )
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven( "https://jitpack.io" )
        gradlePluginPortal()
        mavenCentral()
    }
}