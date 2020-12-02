package sibwaf.kawa

import java.math.BigInteger
import java.nio.file.Paths
import java.security.MessageDigest

private fun String.stableHash(): BigInteger {
    return MessageDigest
            .getInstance("SHA-256")
            .digest(toByteArray())
            .let { BigInteger(it) }
}

fun main(args: Array<String>) {
    val sources = collectSources(args.map { Paths.get(it) })

    val hash = sources.map { it.absolutePath.stableHash() }
            .reduce(BigInteger::add)
            .toString()
            .takeLast(32)

    val model = ModelLoader(hash, sources)

    val report = Analyzer(model, 4).analyze().map { it.wrap() }

    for (warning in report.sortedWith(ReportManager.WARNING_COMPARATOR)) {
        with(warning) {
            println("[$rule] $message. ${position.file}:${position.line}")
        }
    }
}