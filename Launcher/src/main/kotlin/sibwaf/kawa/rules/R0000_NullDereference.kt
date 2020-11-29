package sibwaf.kawa.rules

import sibwaf.kawa.UnreachableFrame
import sibwaf.kawa.constraints.Nullability
import sibwaf.kawa.constraints.ReferenceConstraint
import sibwaf.kawa.values.Value
import spoon.reflect.code.CtFieldRead
import spoon.reflect.code.CtFieldWrite
import spoon.reflect.code.CtInvocation
import spoon.reflect.code.CtTargetedExpression
import spoon.reflect.code.CtThisAccess
import spoon.reflect.code.CtVariableRead
import spoon.reflect.code.CtVariableWrite
import spoon.reflect.declaration.CtMethod
import spoon.reflect.reference.CtTypeReference

class R0000_NullDereference : Rule() {

    private val notifiedValues = mutableSetOf<Value>()

    override fun <T : Any?> visitCtMethod(m: CtMethod<T>?) {
        notifiedValues.clear()
    }

    private fun checkDereference(expression: CtTargetedExpression<*, *>) {
        val flow = getFlow(expression) ?: return
        val frame = getFrame(flow, expression)?.takeUnless { it is UnreachableFrame } ?: return

        val target = expression.target
                ?.takeUnless { it is CtThisAccess<*> }
                ?.takeUnless { it is CtTypeReference<*> }
                ?: return

        val (value, constraint) = getValue(frame, target)
        if (constraint !is ReferenceConstraint) {
            return
        }

        if (constraint.nullability == Nullability.ALWAYS_NULL) {
            if (notifiedValues.add(value)) {
                warn("Null dereference of '${toSimpleString(target)}'", expression)
            }
        }

        if (constraint.nullability == Nullability.POSSIBLE_NULL) {
            if (notifiedValues.add(value)) {
                warn("Possible null dereference of '${toSimpleString(target)}'", expression)
            }
        }
    }

    override fun <T : Any> visitCtInvocation(invocation: CtInvocation<T>) {
        checkDereference(invocation)
    }

    override fun <T : Any> visitCtVariableRead(variableRead: CtVariableRead<T>) {
        val expression = variableRead as? CtFieldRead<*> ?: return
        checkDereference(expression)
    }

    override fun <T : Any> visitCtVariableWrite(variableWrite: CtVariableWrite<T>) {
        val expression = variableWrite as? CtFieldWrite<*> ?: return
        checkDereference(expression)
    }
}