package sibwaf.kawa.rules

import sibwaf.kawa.DataFrame
import sibwaf.kawa.MethodFlow
import sibwaf.kawa.Warning
import spoon.reflect.code.CtBlock
import spoon.reflect.code.CtStatement
import spoon.reflect.declaration.CtElement
import spoon.reflect.declaration.CtExecutable
import spoon.reflect.visitor.CtAbstractVisitor

abstract class Rule : CtAbstractVisitor() {

    lateinit var flow: Map<CtExecutable<*>, MethodFlow>

    private val internalWarnings = mutableListOf<Warning>()
    val warnings: Collection<Warning>
        get() = internalWarnings

    val name by lazy { javaClass.simpleName.takeWhile { it != '_' } }

    fun getFlow(element: CtElement): MethodFlow? {
        return element.getParent(CtExecutable::class.java)
                ?.let { flow[it] }
    }

    fun getFrame(flow: MethodFlow, element: CtElement): DataFrame? {
        var currentElement: CtElement? = element
        while (currentElement != null) {
            val frame = flow.frames[currentElement]
            if (frame != null) {
                return frame
            }

            currentElement = currentElement.parent
        }

        return null
    }

    fun getStatement(element: CtElement): CtStatement? {
        var statement: CtElement = element
        while (statement.parent !is CtBlock<*>) {
            statement = statement.parent ?: return null
        }
        return statement as CtStatement
    }

    fun warn(message: String, element: CtElement) {
        internalWarnings += Warning(name, element, message)
    }
}