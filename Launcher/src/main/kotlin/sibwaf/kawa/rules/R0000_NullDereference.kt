package sibwaf.kawa.rules

import sibwaf.kawa.constraints.Nullability
import sibwaf.kawa.constraints.ReferenceConstraint
import spoon.reflect.code.CtFieldRead
import spoon.reflect.code.CtFieldWrite
import spoon.reflect.code.CtInvocation
import spoon.reflect.code.CtTargetedExpression
import spoon.reflect.code.CtVariableRead
import spoon.reflect.code.CtVariableWrite

class R0000_NullDereference : Rule() {

    private fun checkDereference(expression: CtTargetedExpression<*, *>) {
        val flow = getFlow(expression) ?: return
        val frame = getFrame(flow, expression) ?: return

        val target = (expression.target as? CtVariableRead<*>)?.variable?.declaration ?: return
        val constraint = frame.getConstraint(target) as? ReferenceConstraint ?: return

        if (constraint.nullability == Nullability.ALWAYS_NULL) {
            warn("Null dereference of '${target.simpleName}'", expression)
        }

        if (constraint.nullability == Nullability.POSSIBLE_NULL) {
            warn("Possible null dereference of '${target.simpleName}'", expression)
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