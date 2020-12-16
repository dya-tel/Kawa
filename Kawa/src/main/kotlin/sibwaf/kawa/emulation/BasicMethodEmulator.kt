package sibwaf.kawa.emulation

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.UnreachableFrame
import sibwaf.kawa.constraints.Constraint
import sibwaf.kawa.values.ConstrainedValue
import sibwaf.kawa.values.Value
import sibwaf.kawa.values.ValueSource
import spoon.reflect.reference.CtExecutableReference

class BasicMethodEmulator : MethodEmulator {

    override suspend fun emulate(
        state: AnalyzerState,
        method: CtExecutableReference<*>,
        arguments: List<ConstrainedValue>
    ): InvocationResult {
        val flow = state.getMethodFlow(method)
        if (flow.neverReturns) {
            // TODO: invalid value
            return SuccessfulInvocation(
                frame = UnreachableFrame.after(state.frame),
                value = ConstrainedValue.from(method.type, ValueSource.NONE)
            )
        }

        val result = if (method.executableDeclaration.type.qualifiedName != "void") {
            val value = Value.from(method.type, ValueSource.NONE)
            val constraint = flow.returnConstraint?.copy() ?: Constraint.from(value)
            ConstrainedValue(value, constraint)
        } else {
            null
        }

        // TODO: invocation side-effects

        return SuccessfulInvocation(MutableDataFrame(state.frame), result)
    }
}