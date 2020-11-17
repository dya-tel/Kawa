package sibwaf.kawa

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

private fun collectSources(root: Path): Collection<File> {
    return Files.walk(root, 1)
            .map { it.resolve("src").resolve("main").resolve("java") }
            .filter { Files.exists(it) }
            .filter {
                Files.walk(it).noneMatch { file -> file.fileName.toString() == "module-info.java" }
            }
            .map { it.toFile() }
            .collect(Collectors.toList())
}

val EVENT_BUS = ModelLoader("event-bus", "EventBus-master/EventBus/src")
val AZURE_MAVEN_PLUGINS = ModelLoader(
        "azure-maven-plugins",
        collectSources(Paths.get("azure-maven-plugins-develop"))
)
val HIBERNATE = ModelLoader(
        "hibernate",
        collectSources(Paths.get("hibernate-orm-master"))
)

val project = HIBERNATE
val diffOnly = true

fun main() {
    val report = Analyzer(project, 4).analyze().map { it.wrap() }

    ReportManager.saveReport(project.name, report)

    if (diffOnly) {
        for ((warning, type) in ReportManager.getDiffWithReference(project.name, report)) {
            with(warning) {
                println("[$rule] $type $message. ${position.file}:${position.line}")
            }
        }
    } else {
        for (warning in report) {
            with(warning) {
                println("[$rule] $message. ${position.file}:${position.line}")
            }
        }
    }
}