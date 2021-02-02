package sibwaf.kawa

import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.nio.file.Paths
import java.security.MessageDigest

private fun String.stableHash(): BigInteger {
    return MessageDigest
        .getInstance("SHA-256")
        .digest(toByteArray())
        .let { BigInteger(it) }
}

private val log = LoggerFactory.getLogger("FlowLauncher")

fun main(args: Array<String>) {
    val model = run {
        if (args.size < 2) {
            log.error("At least 2 arguments are required: project_root source_path [source_path ...]")
            return
        }

        val root = Paths.get(args[0])
        val sources = collectSources(args.drop(1).map { Paths.get(it) })
        val hash = sources.map { it.absolutePath.stableHash() }
            .reduce(BigInteger::add)
            .toString()
            .takeLast(32)

        ModelLoader(hash, root, sources)
    }

    val root = Paths.get(model.model.root)
    val report = Analyzer(model, 4)
        .analyze()
        .mapNotNull { it.wrap(root) }

    for (warning in report.sortedWith(ReportManager.WARNING_COMPARATOR)) {
        with(warning) {
            println("[$rule] $message. ${root.resolve(position.file)}:${position.line}")
        }
    }
}