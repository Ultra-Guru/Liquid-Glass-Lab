import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ultraguru.liquidglass.plugin.cloudy"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ultraguru.liquidglass.plugin.cloudy"
        minSdk = 33
        targetSdk = 36
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
    compileOnly(project(":plugin-api"))
    compileOnly(platform(libs.compose.bom))
    compileOnly(libs.compose.ui)
    compileOnly(libs.compose.foundation)
    compileOnly(libs.compose.material3)
    compileOnly(libs.androidx.core.ktx)

    // 库本体：skydoves/Cloudy（com.github.skydoves:cloudy:1.0.0-alpha01）
    implementation(libs.glass.cloudy)
}
