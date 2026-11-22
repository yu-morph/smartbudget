import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
}

abstract class GenerateBuildMetadataTask : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val buildDateTime =
            ZonedDateTime
                .now(ZoneId.of("Asia/Seoul"))
                .format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 a h:mm", Locale.KOREAN))
        val outputFile =
            outputDirectory
                .file("com/yu_morph/smartbudget/ui/main/BuildMetadata.kt")
                .get()
                .asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            package com.yu_morph.smartbudget.ui.main

            internal object BuildMetadata {
                const val BUILD_DATETIME = "$buildDateTime"
            }
            """.trimIndent(),
        )
    }
}

abstract class GenerateOpenSourceLicensesTask : DefaultTask() {
    @get:Input
    abstract val runtimeComponents: ListProperty<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:InputDirectory
    abstract val licenseTextDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        outputDirectory.get().asFile.listFiles()?.forEach { it.deleteRecursively() }
        val outputFile = outputDirectory.file("open_source_licenses.md").get().asFile
        outputFile.parentFile.mkdirs()
        val entries = runtimeComponents.get().distinct().sorted()
        val components = entries.map(::componentInfo)
        val licenses = components.map { it.license }.distinctBy { it.id }
        outputFile.writeText(
            buildString {
                appendLine("# 오픈소스 라이선스")
                appendLine()
                appendLine("SmartBudget 앱에 포함되는 런타임 오픈소스 소프트웨어 목록입니다.")
                appendLine()
                appendLine("| 이름 | 버전 | 라이선스 | 저작권자 |")
                appendLine("| --- | --- | --- | --- |")
                components.forEach { component ->
                    appendLine(
                        "| ${component.name} | ${component.version} | " +
                            "${component.license.name} | ${component.copyrightOwner} |",
                    )
                }
                appendLine()
                appendLine("# 라이선스 본문")
                appendLine()
                licenses.forEach { license ->
                    appendLine("## ${license.name}")
                    appendLine()
                    appendLine(
                        licenseTextDirectory
                            .file(license.textFileName)
                            .get()
                            .asFile
                            .readText()
                            .trim(),
                    )
                    appendLine()
                }
            },
        )
    }

    private fun componentInfo(component: String): ComponentInfo {
        val parts = component.split(':', limit = 3)
        val group = parts[0]
        return ComponentInfo(
            name = "${parts[0]}:${parts[1]}",
            version = parts[2],
            license = licenseFor(group),
            copyrightOwner = copyrightOwnerFor(group),
        )
    }

    private fun licenseFor(group: String): License = when {
        group == "com.kizitonwose.calendar" -> License("mit", "MIT License", "mit.txt")

        group == "com.github.blongho" -> License("mit", "MIT License", "mit.txt")

        group == "org.slf4j" -> License("mit", "MIT License", "mit.txt")

        group == "org.checkerframework" -> License("mit", "MIT License", "mit.txt")

        group.startsWith("androidx.") ||
            group == "com.google.auto.value" ||
            group == "com.google.code.findbugs" ||
            group == "com.google.code.gson" ||
            group == "com.google.crypto.tink" ||
            group == "com.google.dagger" ||
            group == "com.google.errorprone" ||
            group == "com.google.guava" ||
            group == "com.google.j2objc" ||
            group == "com.google.accompanist" ||
            group == "com.caverock" ||
            group == "com.squareup.okio" ||
            group == "io.coil-kt.coil3" ||
            group == "io.ktor" ||
            group == "org.jetbrains" ||
            group == "org.jetbrains.kotlin" ||
            group == "org.jetbrains.kotlinx" ||
            group == "jakarta.inject" ||
            group == "javax.inject" ||
            group == "org.jspecify" ->
            License("apache-2.0", "Apache License 2.0", "apache-2.0.txt")

        else -> error("라이선스 정보가 등록되지 않은 런타임 종속성 그룹입니다: $group")
    }

    private fun copyrightOwnerFor(group: String): String = when {
        group.startsWith("androidx.") -> "The Android Open Source Project"
        group == "com.google.code.findbugs" -> "JSR-305 전문가 그룹 및 FindBugs 기여자"
        group == "com.google.dagger" -> "The Dagger Authors"
        group == "jakarta.inject" || group == "javax.inject" -> "JSR-330 전문가 그룹"
        group.startsWith("com.google.") -> "Google LLC"
        group == "com.kizitonwose.calendar" -> "Kizito Nwose 및 기여자"
        group == "com.github.blongho" -> "Bernard Longho 및 기여자"
        group == "com.caverock" -> "Paul LeBeau"
        group == "com.squareup.okio" -> "Square, Inc. 및 기여자"
        group == "io.coil-kt.coil3" -> "Coil Contributors"
        group == "io.ktor" -> "JetBrains s.r.o. 및 기여자"
        group == "org.jetbrains" || group.startsWith("org.jetbrains.") -> "JetBrains s.r.o. 및 기여자"
        group == "org.jspecify" -> "JSpecify 기여자"
        group == "org.slf4j" -> "QOS.ch 및 SLF4J 기여자"
        group == "org.checkerframework" -> "Checker Framework 개발자"
        else -> error("저작권자 정보가 등록되지 않은 런타임 종속성 그룹입니다: $group")
    }

    private data class ComponentInfo(
        val name: String,
        val version: String,
        val license: License,
        val copyrightOwner: String,
    )

    private data class License(val id: String, val name: String, val textFileName: String)
}

val appVersionName = providers.gradleProperty("appVersionName").orElse("1.0")
val appVersionCode = providers.gradleProperty("appVersionCode").map(String::toInt).orElse(1)
val releaseKeystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH")
val releaseKeystorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD")
val releaseKeyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS")
val releaseKeyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD")
val generateBuildMetadata = tasks.register<GenerateBuildMetadataTask>("generateBuildMetadata") {
    outputDirectory.set(layout.buildDirectory.dir("generated/source/buildMetadata"))
    outputs.upToDateWhen { false }
}

val generateOpenSourceLicenses =
    tasks.register<GenerateOpenSourceLicensesTask>("generateOpenSourceLicenses") {
        runtimeComponents.set(
            providers.provider {
                configurations
                    .getByName("releaseRuntimeClasspath")
                    .incoming
                    .artifactView {}
                    .artifacts
                    .resolvedArtifacts
                    .get()
                    .mapNotNull { artifact ->
                        val identifier = artifact.id.componentIdentifier as? ModuleComponentIdentifier
                        identifier?.let { "${it.group}:${it.module}:${it.version}" }
                    }
            },
        )
        licenseTextDirectory.set(rootProject.layout.projectDirectory.dir("config/licenses"))
        outputDirectory.set(layout.buildDirectory.dir("generated/assets/openSourceLicenses"))
    }

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

    signingConfigs {
        create("release") {
            if (releaseKeystorePath.isPresent) {
                storeFile = file(releaseKeystorePath.get())
                storePassword = releaseKeystorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
            }
        }
    }
    buildTypes {
        release {
            if (releaseKeystorePath.isPresent) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            optimization {
                enable = true
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

    sourceSets.named("main") {
        assets.directories.add(rootProject.layout.projectDirectory.dir("assets/banks").asFile.absolutePath)
    }
}

androidComponents {
    onVariants { variant ->
        variant.sources.java?.addGeneratedSourceDirectory(generateBuildMetadata) { it.outputDirectory }
        variant.sources.assets?.addGeneratedSourceDirectory(generateOpenSourceLicenses) { it.outputDirectory }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.security.crypto)
    implementation(libs.android.svg)
    implementation(libs.calendar.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor3)
    implementation(libs.world.country.data)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    testImplementation(libs.junit)
    testImplementation(libs.ktor.client.mock)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
