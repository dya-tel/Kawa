import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.10"
}

subprojects {
    repositories {
        jcenter()
        mavenCentral()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        kotlinOptions.jvmTarget = "1.8"
    }
}

/*
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("fr.inria.gforge.spoon:spoon-core:8.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    implementation("ch.qos.logback:logback-classic:1.2.3")

    testImplementation(kotlin("test-common"))
    testImplementation(kotlin("test-annotations-common"))
    testImplementation(kotlin("test-junit"))
    testImplementation("io.strikt:strikt-core:0.28.0")

    constraints {
        implementation(kotlin("reflect"))
    }
}
*/
