package sibwaf.kawa.rules

import kotlinx.coroutines.runBlocking
import sibwaf.kawa.MethodFlow
import sibwaf.kawa.ReachableFrame
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
        val executable = if (element is CtExecutable<*>) {
            element
        } else {
            element.getParent(CtExecutable::class.java)
        }

        return executable?.let { flow[it] }
    }

    fun getFrame(flow: MethodFlow, element: CtElement): ReachableFrame? {
        return flow.frames[element]
    }

    fun getValue(flow: MethodFlow, expression: CtExpression<*>): ConstrainedValue? {
        return flow.expressions[expression]
    }

    fun calculateValue(frame: ReachableFrame, expression: CtExpression<*>): ConstrainedValue {
        return runBlocking {
            ValueCalculator.calculateValue(frame, expression, flow)
        }.second
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