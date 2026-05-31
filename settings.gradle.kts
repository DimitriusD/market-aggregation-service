rootProject.name = "market-aggregation-service"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

include(":application")
include(":infrastructure:app")
include(":infrastructure:event-adapter")