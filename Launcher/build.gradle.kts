plugins {
    kotlin("jvm")
}

// TODO
class GitPullTask : Exec() {

    @get:Input
    var url: String? = null

    @get:Input
    var tag: String = "master"

    override fun exec() {
        check(url != null) { "No repository URL provided" }

        val name = url!!.removeSuffix(".git").takeLastWhile { it != '/' } + "-$tag"
        outputs.dir(workingDir.resolve(name))

        commandLine = listOf("git", "clone", "--depth", "1", "--branch", tag, url, name)
        super.exec()
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
