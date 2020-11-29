package sibwaf.kawa.rules

import sibwaf.kawa.UnreachableFrame
import sibwaf.kawa.constraints.Nullability
import sibwaf.kawa.constraints.ReferenceConstraint
import spoon.reflect.code.CtLiteral
import spoon.reflect.code.CtReturn

class R0003_ImplicitNullReturn : Rule() {

    override fun <R : Any?> visitCtReturn(returnStatement: CtReturn<R>) {
        val flow = getFlow(returnStatement) ?: return
        val frame = getFrame(flow, returnStatement)?.takeUnless { it is UnreachableFrame } ?: return
        val expression = returnStatement.returnedExpression.takeUnless { it is CtLiteral<*> } ?: return

        val (_, constraint) = getValue(frame, expression)
        if (constraint is ReferenceConstraint && constraint.nullability == Nullability.ALWAYS_NULL) {
            warn("The returned value is always null: '${toSimpleString(expression)}'", expression);
        }
    }
}