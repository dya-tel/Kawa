plugins {
    kotlin("jvm")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    implementation("fr.inria.gforge.spoon:spoon-core:8.1.0")
    implementation("org.reflections:reflections:0.9.12")
    implementation("com.google.code.gson:gson:2.8.6")

    implementation(project(":Kawa"))
}
