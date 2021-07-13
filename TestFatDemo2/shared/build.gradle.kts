import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.PrintStream

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.5.20"
    kotlin("kapt")
    id("com.android.library")
    id("kotlin-parcelize")
}

/**
 * true : Test Library
 * false : Test Framework
 */
val isTestLibrary = false

val frameworkName = "testCinteropExtFmwkOrLib"

val repoPath = "${projectDir.parent}/CTRepo"

val fDefName = "testFramework"
val frameworkDefFile = project.file("src/nativeInterop/cinterop/$fDefName.def")

val libDefName = "testLibrary"
val libraryDefFile = project.file("src/nativeInterop/cinterop/$libDefName.def")


val frameworkDefConfig = arrayOf(
    "${projectDir.parent}/extFramework",
    "FA.framework",
    "AAA.h",
    "-I${projectDir.parent}/extFramework/FA.framework/Headers"
)

val libraryDefConfig = arrayOf(
    "${projectDir.parent}/extLibrary",
    "libKMMLibrary.a",
    "CInteropTest1.h",
    "-I${projectDir.parent}/extLibrary/include"
)


//iosframework(x86_64 arm64) cinterop to Klib .def
fun writeFrameworkIOSAllDef() {
    if (!frameworkDefFile.exists()) {
        if (!frameworkDefFile.parentFile.exists()) {
            frameworkDefFile.parentFile.mkdirs()
        }
        frameworkDefFile.createNewFile()
    }
    if (frameworkDefFile.exists()) {
        try {
            val (fPath, fName, fHeaders, fCompilerOpts) = frameworkDefConfig
            PrintStream(FileOutputStream(frameworkDefFile)).use {
                it.println(
                    "language = Objective-C\n" +
                            "headers = $fHeaders\n" +
                            "libraryPaths = $fPath\n" +
                            "staticLibraries = $fName\n" +
                            "compilerOpts = $fCompilerOpts\n"
                )
            }
        } catch (e: java.io.FileNotFoundException) {
            println("create framework.def fail")
        }
    }
}


//ios library(x86_64 arm64) cinterop to Klib .def
fun writeLibraryIOSAllDef() {
    if (!libraryDefFile.exists()) {
        if (!libraryDefFile.parentFile.exists()) {
            libraryDefFile.parentFile.mkdirs()
        }
        libraryDefFile.createNewFile()
    }
    if (libraryDefFile.exists()) {
        try {
            val (libPath, libName, libHeaders, libCompilerOpts) = libraryDefConfig
            PrintStream(FileOutputStream(libraryDefFile)).use {
                it.println(
                    "language = Objective-C\n" +
                            "headers = $libHeaders\n" +
                            "libraryPaths = $libPath\n" +
                            "staticLibraries = $libName\n" +
                            "compilerOpts = $libCompilerOpts\n"
                )
            }
        } catch (e: java.io.FileNotFoundException) {
            println("create library.def fail")
        }
    }
}


fun setupIOSCinterop(target: KotlinNativeTarget, defF: File, pkgName: String) {
    target.compilations["main"].cinterops.create(name) {
        defFile = defF
        packageName = "com.yy.$pkgName"
    }
}

fun setupIOSConfig(target: KotlinNativeTarget) {
    target.binaries {
        framework {
            baseName = frameworkName
            isStatic = true
            freeCompilerArgs = listOf("-Xallocator=mimalloc")
        }
    }
    if (isTestLibrary)
        setupIOSCinterop(target, libraryDefFile, "testLibrary")
    else
        setupIOSCinterop(target, frameworkDefFile, "testFramework")
}

kotlin {
    android()

    // iOS Config START

    if (isTestLibrary)
        writeLibraryIOSAllDef()
    else
        writeFrameworkIOSAllDef()

    iosArm64 {
        setupIOSConfig(this)
    }

    iosX64 {
        setupIOSConfig(this)
    }


//    val iosTarget: (String, KotlinNativeTarget.() -> Unit) -> KotlinNativeTarget =
//        if (System.getenv("SDK_NAME")?.startsWith("iphoneos") == true)
//            ::iosArm64
//        else
//            ::iosX64
//
//    iosTarget("ios") {
//        binaries {
//            framework {
//                baseName = "shared"
//            }
//        }
//    }
    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val androidMain by getting
        val androidTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("junit:junit:4.13.2")
            }
        }
        val iosX64Main by getting {
            kotlin.srcDirs("src/iosMain/kotlin")
        }

        val iosArm64Main by getting {
            kotlin.srcDirs("src/iosMain/kotlin")
        }
//        val iosX64Test by getting {
//            kotlin.srcDirs("src/iosTest/kotlin")
//        }
    }
}

android {
    compileSdkVersion(30)
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdkVersion(29)
        targetSdkVersion(30)
    }
}

tasks.getByName("assemble") {
    val finalFilePath = "$buildDir/bin/final"
    val armF = "$buildDir/bin/iosArm64/releaseFramework/$frameworkName.framework"
    val x64F = "$buildDir/bin/iosX64/releaseFramework/$frameworkName.framework"
    doLast {
        exec {
            commandLine("rm", "-rf", finalFilePath)
            commandLine("mkdir", "-p", finalFilePath)
        }
        // remove  X64 -> Arm64
        exec {
            commandLine(
                "lipo",
                "$x64F/$frameworkName",
                "-remove",
                "arm64",
                "-output",
                "$x64F/$frameworkName"
            )
        }

        // remove Arm64-> X64
        exec {
            commandLine(
                "lipo",
                "$armF/$frameworkName",
                "-remove",
                "x86_64",
                "-output",
                "$armF/$frameworkName"
            )
        }

        exec {
            commandLine("cp", "-r", armF, finalFilePath)
        }

        // lipo create  Arm64 & X64
        exec {
            commandLine(
                "lipo",
                "-create",
                "$finalFilePath/$frameworkName.framework/$frameworkName",
                "$x64F/$frameworkName",
                "-output",
                "$finalFilePath/$frameworkName.framework/$frameworkName"
            )
        }
    }
}