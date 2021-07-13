buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.20")
        classpath("com.android.tools.build:gradle:4.2.2")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }

    /**
    If if shared Module's build.gradle.kts config isTestLibrary = true,
    Fat task must be filtered, otherwise it will fail

    Because if TestLibrary=True,
    xxx.framework under iosArm64 in the build directory of this project will contain X64,
    and xxx.framework under iosX64 will also contain arm64,
    which is the direct cause of the failure of lipo-create task in fat
     */

//    gradle.taskGraph.whenReady {
//        tasks.forEach { task ->
//            if (task.name.endsWith("fat", true)) {
//                task.enabled = false
//            }
//        }
//    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}