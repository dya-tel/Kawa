package sibwaf.kawa

import spoon.reflect.declaration.CtElement

data class Warning(
        val rule: String,
        val element: CtElement,
        val message: String
) {

    fun wrap(): SerializableWarningWrapper {
        val position = PositionSignature(
                file = element.position.file.absolutePath,
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