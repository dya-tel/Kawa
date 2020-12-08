package sibwaf.kawa.emulation

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
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
    ): Pair<DataFrame, ConstrainedValue?> {
        val flow = state.getMethodFlow(method)
        if (flow.neverReturns) {
            // TODO: invalid value
            return UnreachableFrame.after(state.frame) to ConstrainedValue.from(method.type, ValueSource.NONE)
        }

        val value = Value.from(method.type, ValueSource.NONE)
        val constraint = flow.returnConstraint?.copy() ?: Constraint.from(value)

        // TODO: invocation side-effects

        return MutableDataFrame(state.frame) to ConstrainedValue(value, constraint)
    }
}