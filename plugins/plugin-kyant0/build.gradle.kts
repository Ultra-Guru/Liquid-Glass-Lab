import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ultraguru.liquidglass.plugin.kyant0"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ultraguru.liquidglass.plugin.kyant0"
        minSdk = 33
        targetSdk = 36
        // 插件自己的版本（release.yml 注入；插件可独立于宿主发版）
        versionCode = (findProperty("lgPluginVersionCode") as String?)?.toInt() ?: 1
        versionName = (findProperty("lgPluginVersionName") as String?) ?: "0.1.0"
    }

    val keystoreB64 = providers.environmentVariable("ANDROID_KEYSTORE_B64").orNull
    val keystorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
    val keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
    val keyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull

    signingConfigs {
        if (keystoreB64 != null && keystorePassword != null && keyAlias != null && keyPassword != null) {
            create("release") {
                val ksFile = layout.buildDirectory.file("signing/liquidglass-release.keystore").get().asFile
                ksFile.parentFile.mkdirs()
                ksFile.writeBytes(Base64.getDecoder().decode(keystoreB64))
                storeFile = ksFile
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    // 契约与 Compose 运行时由宿主提供（parent classloader），插件 APK 保持小体积
    compileOnly(project(":plugin-api"))
    compileOnly(platform(libs.compose.bom))
    compileOnly(libs.compose.ui)
    compileOnly(libs.compose.foundation)
    compileOnly(libs.compose.material3)
    compileOnly(libs.androidx.core.ktx)
    compileOnly(libs.compose.material.icons)

    // 库本体：kyant0 AndroidLiquidGlass 1.0.6 + Kyant0/Shapes 1.2.0 —— vendored 源码内嵌
    //（上游 shapes 的 Maven Android 变体为空发布，二进制消费不可用；同 AGA 项目的 vendor 决策）
    // 源码位于 src/main/kotlin/.../vendor/{backdrop,shapes}/，含上游 LICENSE
}
