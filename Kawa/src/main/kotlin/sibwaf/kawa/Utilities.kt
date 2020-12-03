package sibwaf.kawa

import spoon.reflect.code.CtBlock
import spoon.reflect.code.CtIf
import spoon.reflect.declaration.CtElement
import spoon.reflect.visitor.filter.TypeFilter
import java.util.Collections
import java.util.IdentityHashMap
import java.util.Stack
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.min

typealias BinaryOperation<T> = T.(T) -> T

@Suppress("FunctionName")
fun <T> IdentityHashSet(): MutableSet<T> = Collections.newSetFromMap(IdentityHashMap())

@Suppress("FunctionName")
fun <T> ConcurrentHashSet(): MutableSet<T> = Collections.newSetFromMap(ConcurrentHashMap())

fun <T> List<T>.splitIntoParts(parts: Int): List<List<T>> {
    val size = size
    val countPerPart = ceil(size.toDouble() / parts).toInt()
    return (0 until parts).map { index ->
        val start = index * countPerPart
        if (start >= size) {
            emptyList()
        } else {
            subList(start, min(size, (index + 1) * countPerPart))
        }
    }
}

class RightChain<T>(val previous: RightChain<T>?, val current: T) {
    fun contains(element: T): Boolean {
        var node: RightChain<T>? = this
        while (node != null) {
            if (node.current === element) {
                return true
            }

            node = node.previous
        }

        return false
    }
}

inline fun <T> Stack<T>.replaceTop(value: T): T {
    val oldValue = pop()
    push(value)
    return oldValue
}

inline fun <reified T : CtElement> CtElement.getElementsOf(): List<T> = getElements(TypeFilter(T::class.java))

inline val CtIf.thenBlock: CtBlock<*>
    get() = getThenStatement()

inline val CtIf.elseBlock: CtBlock<*>?
    get() = getElseStatement()