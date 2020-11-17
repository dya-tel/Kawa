package sibwaf.kawa

import spoon.Launcher
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtLocalVariable
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

fun parseExpression(text: String, vararg parameters: String) = parseExpression(text, parameters.asIterable())

fun parseExpression(text: String, parameters: Iterable<String>): CtExpression<*> {
    val method = parseMethod(
            """
            void test(${parameters.joinToString()}) {
                Object x = $text;
            }
            """.trimIndent()
    )

    val variable = method.getElementsOf<CtLocalVariable<*>>().single()
    return variable.defaultExpression
}

fun CtElement.extractVariables(): Map<String, CtVariable<*>> {
    return getElementsOf<CtVariable<*>>()
            .groupBy { it.simpleName }
            .mapValues { (_, value) -> value.single() }
}

//inline fun <reified T : CtElement> CtElement.getElementsOf(): List<T> = getElements(TypeFilter(T::class.java))