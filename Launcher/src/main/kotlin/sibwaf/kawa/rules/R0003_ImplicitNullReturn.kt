package sibwaf.kawa.rules

import sibwaf.kawa.constraints.Nullability
import sibwaf.kawa.constraints.ReferenceConstraint
import spoon.reflect.code.CtLiteral
import spoon.reflect.code.CtReturn

class R0003_ImplicitNullReturn : Rule() {

    override fun <R : Any?> visitCtReturn(returnStatement: CtReturn<R>) {
        val flow = getFlow(returnStatement) ?: return
        val expression = returnStatement.returnedExpression.takeUnless { it is CtLiteral<*> } ?: return

        val (_, constraint) = getValue(flow, expression) ?: return
        if (constraint is ReferenceConstraint && constraint.nullability == Nullability.ALWAYS_NULL) {
            warn("The returned value is always null: '${toSimpleString(expression)}'", expression);
        }
    }
}