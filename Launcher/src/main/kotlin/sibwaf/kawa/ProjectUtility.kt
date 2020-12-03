package sibwaf.kawa

import java.io.File
import java.nio.file.Files
import java.nio.file.Path

fun collectSources(vararg paths: Path): Collection<File> {
    return collectSources(paths.asIterable())
}

fun collectSources(paths: Iterable<Path>): Collection<File> {
    return paths.asSequence()
        .flatMap { Files.walk(it, 1).iterator().asSequence() }
        .plus(paths)
        .map { it.resolve("src").resolve("main").resolve("java") }
        .filter { Files.exists(it) }
        .filter {
            Files.walk(it).noneMatch { file -> file.fileName.toString() == "module-info.java" }
        }
        .map { it.toFile() }
        .toList()
}