/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalWasmDsl::class)

import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.native.tasks.*
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.testing.*

plugins {
    kotlin("multiplatform")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 8
}

kotlin {
    explicitApi()

    jvm {
        withJava()
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget = JvmTarget.JVM_1_8
            freeCompilerArgs.add("-Xjdk-release=1.8")
        }
    }
    jvmToolchain(jdkToolchainVersion)

    js {
        nodejs {
            testTask {
                useMocha {
                    timeout = "10s"
                }
            }
        }

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            sourceMap = true
            moduleKind = JsModuleKind.MODULE_UMD
        }
    }

    wasmJs {
        nodejs()
    }

    wasmWasi {
        nodejs()
    }

    sourceSets.all {
        kotlin.srcDirs("$name/src")
        resources.srcDirs("$name/resources")
        languageSettings {
            progressiveMode = true

            optIn("kotlin.ExperimentalMultiplatform")
            optIn("kotlinx.serialization.InternalSerializationApi")
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-stdlib-common")
            }
        }

        commonTest {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test-common")
                api("org.jetbrains.kotlin:kotlin-test-annotations-common")
            }
        }

        jvmMain {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-stdlib")
            }
        }

        jvmTest {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test-junit")
            }
        }

        jsMain {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-stdlib-js")
            }
        }

        jsTest {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test-js")
            }
        }

        register("wasmMain") {
            dependsOn(commonMain.get())
        }
        register("wasmTest") {
            dependsOn(commonTest.get())
        }

        named("wasmJsMain") {
            dependsOn(named("wasmMain").get())
            dependencies {
                api("org.jetbrains.kotlin:kotlin-stdlib-wasm-js")
            }
        }

        named("wasmJsTest") {
            dependsOn(named("wasmTest").get())
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test-wasm-js")
            }
        }

        named("wasmWasiMain") {
            dependsOn(named("wasmMain").get())
            dependencies {
                api("org.jetbrains.kotlin:kotlin-stdlib-wasm-wasi")
            }
        }

        named("wasmWasiTest") {
            dependsOn(named("wasmTest").get())
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test-wasm-wasi")
            }
        }
    }

    sourceSets.matching({ it.name.contains("Test") }).configureEach {
        languageSettings {
            optIn("kotlinx.serialization.InternalSerializationApi")
            optIn("kotlinx.serialization.ExperimentalSerializationApi")
        }
    }
}

tasks.withType(KotlinCompilationTask::class).configureEach {
    compilerOptions {
        val isMainTaskName = name.startsWith("compileKotlin")
        if (isMainTaskName) {
            allWarningsAsErrors = true
        }
        if (overriddenLanguageVersion != null) {
            languageVersion = KotlinVersion.fromVersion(overriddenLanguageVersion!!)
            freeCompilerArgs.add("-Xsuppress-version-warnings")
        }
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
