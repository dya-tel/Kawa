package sibwaf.kawa

import spoon.Launcher
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtLocalVariable
import spoon.reflect.code.CtStatement
import spoon.reflect.declaration.CtClass
import spoon.reflect.declaration.CtElement
import spoon.reflect.declaration.CtMethod
import spoon.reflect.declaration.CtVariable

fun parse(text: String): CtClass<*> {
    return Launcher.parseClass(text)
}

fun parseMethod(text: String): CtMethod<*> {
    return parse("class A { $text }").methods.single()
}

inline fun <reified T : CtStatement> parseStatement(text: String, vararg parameters: String) =
        parseStatement<T>(text, parameters.asIterable())

inline fun <reified T : CtStatement> parseStatement(text: String, parameters: Iterable<String>): T {
    val method = parseMethod(
            """
            void test(${parameters.joinToString()}) {
                $text;
            }
            """.trimIndent()
    )

    return method.body.getStatement(0)
}

fun parseExpression(text: String, vararg parameters: String) = parseExpression(text, parameters.asIterable())

fun parseExpression(text: String, parameters: Iterable<String>): CtExpression<*> {
    val statement = parseStatement<CtLocalVariable<*>>(
            text = "Object x = $text",
            parameters = parameters
    )

    return statement.defaultExpression
}

fun CtElement.extractVariables(): Map<String, CtVariable<*>> {
    return getElementsOf<CtVariable<*>>()
            .groupBy { it.simpleName }
            .mapValues { (_, value) -> value.single() }
}

//inline fun <reified T : CtElement> CtElement.getElementsOf(): List<T> = getElements(TypeFilter(T::class.java))