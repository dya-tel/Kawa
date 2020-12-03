package sibwaf.kawa

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Comparator
import java.util.IdentityHashMap

enum class DiffType {
    ADDITIONAL, MISSING
}

class ReportManager(private val directory: Path = Paths.get("reports")) {

    companion object {

        private val fileComparator: Comparator<SerializableWarningWrapper> = Comparator.comparing { it.position.file }
        private val positionComparator: Comparator<SerializableWarningWrapper> = Comparator.comparing { it.position.start }
        val WARNING_COMPARATOR = fileComparator.then(positionComparator)

        fun getDiff(
            previous: Collection<SerializableWarningWrapper>,
            current: Collection<SerializableWarningWrapper>
        ): Map<SerializableWarningWrapper, DiffType> {
            val result = IdentityHashMap<SerializableWarningWrapper, DiffType>()

            for (warning in previous) {
                if (warning !in current) {
                    result[warning] = DiffType.MISSING
                }
            }

            for (warning in current) {
                if (warning !in previous) {
                    result[warning] = DiffType.ADDITIONAL
                }
            }

            return result
        }
    }

    private val gson = GsonBuilder().setPrettyPrinting().create()

    private val type = object : TypeToken<Collection<SerializableWarningWrapper>>() {}.type

    fun getReferenceReport(name: String): Collection<SerializableWarningWrapper> {
        val path = directory.resolve("$name.reference.json")

        return if (Files.exists(path)) {
            path.toFile().reader().use {
                gson.fromJson(it, type)
            }
        } else {
            emptyList()
        }
    }

    fun saveReport(name: String, report: Collection<SerializableWarningWrapper>) {
        Files.createDirectories(directory)

        val path = directory.resolve("$name.latest.json")
        path.toFile().writer().use {
            gson.toJson(report, it)
        }
    }

    fun getDiffWithReference(name: String, current: Collection<SerializableWarningWrapper>): Map<SerializableWarningWrapper, DiffType> {
        return getDiff(getReferenceReport(name), current)
    }
}