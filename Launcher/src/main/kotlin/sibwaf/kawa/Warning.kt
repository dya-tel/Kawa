package sibwaf.kawa

import spoon.reflect.declaration.CtElement
import java.nio.file.Path

data class Warning(
    val rule: String,
    val element: CtElement,
    val message: String
) {

    fun wrap(rootPath: Path): SerializableWarningWrapper? {
        val absoluteRoot = rootPath.toAbsolutePath()

        val position = element.position.takeIf { it.isValidPosition } ?: return null

        var filePath = position.file.toPath().toAbsolutePath()
        if (filePath.startsWith(absoluteRoot)) {
            filePath = absoluteRoot.relativize(filePath)
        }

        return SerializableWarningWrapper(
            rule = rule,
            position = PositionSignature(
                file = filePath.toString().replace("\\", "/"),
                line = position.line,
                start = position.sourceStart,
                end = position.sourceEnd
            ),
            message = message
        )
    }
}

data class PositionSignature(
    val file: String,
    val line: Int,
    val start: Int,
    val end: Int
)

data class SerializableWarningWrapper(
    val rule: String,
    val position: PositionSignature,
    val message: String
)