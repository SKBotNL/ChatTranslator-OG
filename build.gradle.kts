import de.undercouch.gradle.tasks.download.Download
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("de.undercouch.download") version "5.6.0"
    id("maven-publish")
    id("eclipse")
}

group = "nl.skbotnl.chatog"
version = "2.1.4"

val apiVersion = "1.19"

publishing {
    publications {
        create<MavenPublication>("mavenPublication") {
            groupId = "nl.skbotnl.chatog"
            artifactId = "Chat-OG"
            version = version
        }
    }
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf(
        "version" to version,
        "apiVersion" to apiVersion,
    )
    filesMatching("plugin.yml") {
        expand(props)
    }
}

repositories {
    mavenCentral()

    maven {
        url = uri("https://repo.purpurmc.org/snapshots")
    }

    maven {
        url = uri("https://jitpack.io")
    }

    maven {
        url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }

    maven {
        url = uri("https://repo.essentialsx.net/releases/")
    }
}

dependencies {
    compileOnly("org.purpurmc.purpur:purpur-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("net.essentialsx:EssentialsX:2.20.1")
    compileOnly(files("libs/AnnouncerPlus-1.3.6.jar"))
    implementation("net.dv8tion:JDA:5.2.2") {
        exclude(module = "opus-java")
    }
    implementation("club.minnced:jda-ktx:0.12.0")
    implementation("club.minnced:discord-webhooks:0.8.4")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("io.github.crackthecodeabhi:kreds:0.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val downloadPython by tasks.creating(Download::class) {
    src("https://github.com/python/cpython/archive/refs/heads/3.11.zip")
    dest(layout.buildDirectory.file("python.zip"))
}

val unzipPython by tasks.creating(Copy::class) {
    dependsOn(downloadPython)
    from(zipTree(downloadPython.dest)) {
        include("cpython*/**")
        eachFile {
            relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
        }
        includeEmptyDirs = false
    }
    into(layout.buildDirectory.dir("python"))
}

val configurePython by tasks.creating(Exec::class) {
    dependsOn(unzipPython)
    doFirst {
        Files.createDirectories(file("$rootDir/build/python/build").toPath())
    }
    workingDir("./build/python")
    commandLine("./configure")
    args(
        "--enable-optimizations"
    )
}

val makePython by tasks.creating(Exec::class) {
    dependsOn(configurePython)
    workingDir("$rootDir/build/python")
    commandLine("sh")
    args(
        "-c",
        "make -j$(nproc)"
    )
}

tasks.register<Exec>("installPython") {
    dependsOn(makePython)
    workingDir("$rootDir/build/python")
    commandLine("make")
    environment("DESTDIR" to "$rootDir/build/python/install")
    args(
        "install"
    )
    doLast {
        file("$rootDir/build/python/install/usr/local/share").deleteRecursively()
        file("$rootDir/build/python/install/usr/local/lib/pkgconfig").deleteRecursively()
        file("$rootDir/build/python.zip").delete()
    }
}

tasks.shadowJar {
    minimize()
}

tasks.shadowJar.configure {
    archiveClassifier.set("")
    if (!file("$rootDir/build/python/python.zip").exists() && file("$rootDir/build/python/install/usr/local/").exists()) {
        val inputDirectory = File("$rootDir/build/python/install/usr/local/")
        val outputZipFile = File("$rootDir/build/python/python.zip")
        outputZipFile.createNewFile()
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZipFile))).use { zos ->
            inputDirectory.walkTopDown().forEach { file ->
                val zipFileName = file.absolutePath.removePrefix(inputDirectory.absolutePath).removePrefix("/")
                val entry = ZipEntry("$zipFileName${(if (file.isDirectory) "/" else "")}")
                zos.putNextEntry(entry)
                if (file.isFile) {
                    file.inputStream().use { fis -> fis.copyTo(zos) }
                }
            }
        }
    }
    if (file("$rootDir/build/python/python.zip").exists()) {
        from("$rootDir/build/python/python.zip")
    }
}

tasks.register<Exec>("buildPython") {
    group = "Chat-OG"
    description = "Builds Python only if run as the sole task or as a pre-build step."
    val isPreBuildRun = System.getenv("PRE_BUILD_PYTHON_RUN")?.toBoolean() ?: false
    onlyIf {
        val isSoleTask = gradle.startParameter.taskNames.contains("buildPython") && gradle.startParameter.taskNames.size == 1
        val shouldRun = isSoleTask || isPreBuildRun
        shouldRun
    }
    if (gradle.startParameter.taskNames.contains("buildPython") &&
        gradle.startParameter.taskNames.size == 1 ||
        isPreBuildRun) {
        dependsOn("installPython")
    }
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.jar.configure {
    archiveClassifier.set("part")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
    options.encoding = "UTF-8"
    options.forkOptions.executable = File(options.forkOptions.javaHome, "bin/javac").path
}

kotlin {
    jvmToolchain(17)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
        vendor = JvmVendorSpec.GRAAL_VM
    }
}
