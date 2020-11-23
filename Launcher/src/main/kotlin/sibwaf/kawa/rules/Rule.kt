package sibwaf.kawa.rules

import kotlinx.coroutines.runBlocking
import sibwaf.kawa.DataFrame
import sibwaf.kawa.EmptyFlow
import sibwaf.kawa.MethodFlow
import sibwaf.kawa.ValueCalculator
import sibwaf.kawa.Warning
import sibwaf.kawa.values.ConstrainedValue
import spoon.reflect.code.CtBlock
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtInvocation
import spoon.reflect.code.CtStatement
import spoon.reflect.code.CtVariableAccess
import spoon.reflect.declaration.CtElement
import spoon.reflect.declaration.CtExecutable
import spoon.reflect.declaration.CtVariable
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

    fun getValue(frame: DataFrame, expression: CtExpression<*>): ConstrainedValue {
        return runBlocking {
            ValueCalculator.calculateValue(MethodFlow(), frame, expression, { flow[it] ?: EmptyFlow })
        }
    }

    fun getStatement(element: CtElement): CtStatement? {
        var statement: CtElement = element
        while (statement.parent !is CtBlock<*>) {
            statement = statement.parent ?: return null
        }
        return statement as CtStatement
    }

    fun toSimpleString(element: CtElement): String {
        return when (element) {
            is CtVariable<*> -> element.simpleName
            is CtVariableAccess<*> -> element.variable.simpleName
            is CtInvocation<*> -> "${element.executable.simpleName}()"
            else -> element.toString()
        }
    }

    fun warn(message: String, element: CtElement) {
        internalWarnings += Warning(name, element, message)
    }
}