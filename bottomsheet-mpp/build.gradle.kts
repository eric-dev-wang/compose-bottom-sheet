@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    android {
        namespace = "com.ericdevwang.bottomsheet.mpp"
        compileSdk { version = release(36) }
    }

    iosArm64()
    iosSimulatorArm64()

    // Import local Swift Package
    swiftPMDependencies {
        iosMinimumDeploymentTarget.set("16.0")
        localSwiftPackage(
            directory = project.layout.projectDirectory.dir("../bottomsheet-spm"),
            products = listOf("BottomSheetSPM")
        )
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.ktx)
            implementation(project(":bottomsheet-androidx"))
        }
    }

    targets
        .withType<KotlinNativeTarget>()
        .matching { it.konanTarget.family.isAppleFamily }
        .configureEach {
            binaries {
                framework {
                    baseName = "BottomSheetMpp"
                    isStatic = true
                }
            }
        }
}
