plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

val appVersionName = providers.gradleProperty("appVersionName").orElse("1.0")
val appVersionCode = providers.gradleProperty("appVersionCode").map(String::toInt).orElse(1)

ktlint {
    android.set(true)
    version.set("1.8.0")
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
}

tasks.matching {
    it.name == "preBuild" ||
        it.name == "detekt" ||
        it.name.startsWith("runKtlintCheck")
}.configureEach {
    mustRunAfter("ktlintFormat")
}

android {
    namespace = "com.yu_morph.smartbudget"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.yu_morph.smartbudget"
        minSdk = 29
        targetSdk = 37
        versionCode = appVersionCode.get()
        versionName = appVersionName.get()

        ndk {
            abiFilters += setOf("arm64-v8a", "armeabi-v7a")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
