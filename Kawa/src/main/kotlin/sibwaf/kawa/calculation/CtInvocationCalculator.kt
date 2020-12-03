package sibwaf.kawa.calculation

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.UnreachableFrame
import sibwaf.kawa.constraints.Constraint
import sibwaf.kawa.values.ConstrainedValue
import sibwaf.kawa.values.Value
import sibwaf.kawa.values.ValueSource
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtInvocation

class CtInvocationCalculator : CtTargetedExpressionCalculator() {

    override fun supports(expression: CtExpression<*>) = expression is CtInvocation<*>

    override suspend fun calculate(
        state: AnalyzerState,
        expression: CtExpression<*>,
        target: ConstrainedValue
    ): Pair<DataFrame, ConstrainedValue> {
        expression as CtInvocation<*>

        var currentState = state
        for (argument in expression.arguments) {
            val (nextFrame, _) = currentState.getValue(argument)
            currentState = state.copy(frame = nextFrame)
        }

        val flow = currentState.getMethodFlow(expression.executable)
        if (flow.neverReturns) {
            return UnreachableFrame.after(currentState.frame) to ConstrainedValue.from(expression, ValueSource.NONE)
        }

        val value = Value.from(expression.type, ValueSource.NONE)
        val constraint = flow.returnConstraint?.copy() ?: Constraint.from(value)

        // TODO: invocation side-effects

        return MutableDataFrame(currentState.frame) to ConstrainedValue(value, constraint)
    }
}