package sibwaf.kawa

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Comparator
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
val ASYNC_HTTP_CLIENT = ModelLoader(
        "async-http-client",
        collectSources(Paths.get("async-http-client-master")) + collectSources(Paths.get("async-http-client-master", "extras"))
)

val project = HIBERNATE
val diffOnly = false

fun main() {
    val report = Analyzer(project, 4).analyze().map { it.wrap() }

    ReportManager.saveReport(project.name, report)

    val fileComparator: Comparator<SerializableWarningWrapper> = Comparator.comparing { it.position.file }
    val positionComparator: Comparator<SerializableWarningWrapper> = Comparator.comparing { it.position.start }
    val warningComparator = fileComparator.then(positionComparator)

    if (diffOnly) {
        val diff = ReportManager.getDiffWithReference(project.name, report)
        val sortedDiff = diff.toSortedMap(warningComparator)

        for ((warning, type) in sortedDiff) {
            with(warning) {
                println("[$rule] $type $message. ${position.file}:${position.line}")
            }
        }
    } else {
        for (warning in report.sortedWith(warningComparator)) {
            with(warning) {
                println("[$rule] $message. ${position.file}:${position.line}")
            }
        }
    }
}