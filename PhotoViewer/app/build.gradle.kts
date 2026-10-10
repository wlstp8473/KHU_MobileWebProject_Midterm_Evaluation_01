import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localProperties = Properties().apply {
    val propertiesFile = rootProject.file("local.properties")

    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use {
            load(it)
        }
    }
}

val localAuthToken = localProperties.getProperty("LOCAL_AUTH_TOKEN")?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?: throw GradleException(
        "PhotoViewer/local.properties에 LOCAL_AUTH_TOKEN을 설정하세요."
    )

val cloudAuthToken = localProperties.getProperty("CLOUD_AUTH_TOKEN")?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?: throw GradleException(
        "PhotoViewer/local.properties에 CLOUD_AUTH_TOKEN을 설정하세요."
    )

require(localAuthToken.matches(Regex("[0-9a-fA-F]{40}"))) {
    "LOCAL_AUTH_TOKEN은 Django REST Framework의 40자리 16진수 토큰이어야 합니다."
}

require(cloudAuthToken.matches(Regex("[0-9a-fA-F]{40}"))) {
    "CLOUD_AUTH_TOKEN은 Django REST Framework의 40자리 16진수 토큰이어야 합니다."
}

android {
    namespace = "com.example.photoviewer"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.photoviewer"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "LOCAL_AUTH_TOKEN",
            "\"$localAuthToken\""
        )

        buildConfigField(
            "String",
            "CLOUD_AUTH_TOKEN",
            "\"$cloudAuthToken\""
        )
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}