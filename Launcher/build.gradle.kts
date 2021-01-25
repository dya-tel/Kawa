import java.io.ByteArrayOutputStream
import java.io.OutputStream

plugins {
    kotlin("jvm")
}

fun gitPull(url: String, tag: String = "master", commit: String?, workingDirectory: String = ".") {
    val name = url.removeSuffix(".git").takeLastWhile { it != '/' }
    val repositoryDir = file(workingDirectory).resolve(name)

    val alreadyCloned = repositoryDir.isDirectory
    if (alreadyCloned) {
        try {
            val output = ByteArrayOutputStream()

            val result = exec {
                workingDir = repositoryDir
                commandLine = listOf("git", "config", "--get", "remote.origin.url")
                standardOutput = output
            }

            result.assertNormalExitValue()
            check(output.toString().trim() == url)
        } catch (e: Exception) {
            val path = repositoryDir.relativeTo(projectDir)
            throw RuntimeException("Local repository '$path' is corrupted. Clean it manually and rerun this task.")
        }
    }

    if (!alreadyCloned) {
        exec {
            workingDir = file(workingDirectory)
            commandLine = listOf("git", "clone", "--branch", tag, url, name)
        }
    }

    if (commit != null) {
        exec {
            workingDir = file(workingDirectory).resolve(name)
            commandLine = listOf("git", "checkout", commit, ".")
        }
    }
}

task("prepareTestProjects") {
    doFirst {
        gitPull(
            "https://github.com/AsyncHttpClient/async-http-client.git",
            commit = "bd7b5bd0e3e5f7eb045d3e996d94b9f190fe3232",
            workingDirectory = "projects"
        )
        gitPull(
            "https://github.com/microsoft/azure-maven-plugins.git",
            commit = "56c3c0fd35f59a07cb9f06ce2c32827ef16c5482",
            workingDirectory = "projects"
        )
        gitPull(
            "https://github.com/greenrobot/EventBus.git",
            commit = "1d995077d0b620a6aae9c60a8b96443113752305",
            workingDirectory = "projects"
        )
        gitPull(
            "https://github.com/hibernate/hibernate-orm.git",
            commit = "e820e4cdfbbdcee1de7ca220b6843bc33fb3dd68",
            workingDirectory = "projects"
        )
        gitPull(
            "https://github.com/magefree/mage.git",
            commit = "a3133089e79acfc7511229ffe0cb33a0b0fd4811",
            workingDirectory = "projects"
        )
    }
}

tasks.withType<Test> {
    maxHeapSize = "2048m"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    implementation("fr.inria.gforge.spoon:spoon-core:8.1.0")
    implementation("org.reflections:reflections:0.9.12")
    implementation("com.google.code.gson:gson:2.8.6")

    implementation(project(":Kawa"))

    testImplementation(kotlin("test-common"))
    testImplementation(kotlin("test-annotations-common"))
    testImplementation(kotlin("test-junit"))
}
