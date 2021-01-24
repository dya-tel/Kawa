package sibwaf.kawa

import spoon.reflect.declaration.CtElement
import java.nio.file.Path

data class Warning(
    val rule: String,
    val element: CtElement,
    val message: String
) {

    fun wrap(rootPath: Path): SerializableWarningWrapper {
        val absoluteRoot = rootPath.toAbsolutePath()

        var filePath = element.position.file.toPath().toAbsolutePath()
        if (filePath.startsWith(absoluteRoot)) {
            filePath = absoluteRoot.relativize(filePath)
        }

        val position = PositionSignature(
            file = filePath.toString().replace("\\", "/"),
            line = element.position.line,
            start = element.position.sourceStart,
            end = element.position.sourceEnd
        )

        return SerializableWarningWrapper(
            rule = rule,
            position = position,
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