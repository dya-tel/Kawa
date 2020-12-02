package sibwaf.kawa

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.fail

abstract class DiffTesterBase {

    val baseProjectPath: Path = Paths.get("projects")

    abstract val model: ModelLoader

    @Test fun test() {
        val report = Analyzer(model, 4).analyze().map { it.wrap() }

        val reportManager = ReportManager()
        reportManager.saveReport(model.name, report)
        val diff = reportManager.getDiffWithReference(model.name, report)

        if (diff.isNotEmpty()) {
            val sortedDiff = diff.toSortedMap(ReportManager.WARNING_COMPARATOR)
            for ((warning, type) in sortedDiff) {
                with(warning) {
                    System.err.println("[$rule] $type $message. ${position.file}:${position.line}")
                }
            }

            fail("Diff is present")
        }
    }
}