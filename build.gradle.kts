plugins {
    kotlin("multiplatform") version "1.9.23"
    id("org.jetbrains.compose") version "1.6.10"
}

group = "com.anne"
version = "1.0-SNAPSHOT"

val isWindows = System.getProperty("os.name").lowercase().contains("windows")
val windowsHelperSource = layout.projectDirectory.file("native/windows/AnneVirtualDesktop.cs")
val windowsHelperResources = layout.buildDirectory.dir("generated/windowsHelperResources")
val windowsHelperExecutable = windowsHelperResources.map {
    it.file("windows/AnneVirtualDesktop.exe")
}

val compileWindowsDesktopHelper by tasks.registering(Exec::class) {
    onlyIf { isWindows }
    inputs.file(windowsHelperSource)
    outputs.file(windowsHelperExecutable)

    doFirst {
        windowsHelperExecutable.get().asFile.parentFile.mkdirs()
    }

    commandLine(
        "C:\\Windows\\Microsoft.NET\\Framework64\\v4.0.30319\\csc.exe",
        "/nologo",
        "/target:winexe",
        "/platform:x64",
        "/optimize+",
        "/out:${windowsHelperExecutable.get().asFile.absolutePath}",
        windowsHelperSource.asFile.absolutePath
    )
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = "21"
        }
    }
    
    sourceSets {
        val desktopMain by getting {
            resources.srcDir(windowsHelperResources)
            dependencies {
                implementation(compose.desktop.currentOs)
                // JNativeHook for global keyboard events
                implementation("com.github.kwhat:jnativehook:2.2.2")
            }
        }
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.ui)
            }
        }
    }
}

tasks.matching { it.name == "desktopProcessResources" }.configureEach {
    dependsOn(compileWindowsDesktopHelper)
}

compose.desktop {
    application {
        mainClass = "com.anne.utils.MainKt"
        
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb)
            packageName = "AnneDesktopUtils"
            packageVersion = "1.0.0"
        }
    }
}
