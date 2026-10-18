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
        // qwea0/Liquid-Glass-Android 仅发布在 JitPack（com.github.QWEA0:liquidglass）。
        // content 过滤：只有该 group 走 JitPack，其余一律 Central/Google，避免供应链漂移。
        maven("https://jitpack.io") {
            content {
                includeGroup("com.github.QWEA0")
            }
        }
    }
}

rootProject.name = "Liquid-Glass-Lab"

include(":app")
include(":plugin-api")
include(":plugins:plugin-kyant0")
include(":plugins:plugin-cloudy")
include(":plugins:plugin-qmdeve")
include(":plugins:plugin-qwea0")
include(":plugins:plugin-nadeem")
