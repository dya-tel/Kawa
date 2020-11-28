package sibwaf.kawa.calculation

import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.constraints.Constraint
import sibwaf.kawa.values.ConstrainedValue
import sibwaf.kawa.values.Value
import sibwaf.kawa.values.ValueSource
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtInvocation

class CtInvocationCalculator : ValueCalculator {

    override fun supports(expression: CtExpression<*>) = expression is CtInvocation<*>

    override suspend fun calculate(state: ValueCalculatorState, expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue> {
        expression as CtInvocation<*>

        var currentState = state
        for (argument in expression.arguments) {
            val (nextFrame, _) = currentState.getValue(argument)
            currentState = state.copy(frame = nextFrame)
        }

        val flow = currentState.getMethodFlow(expression.executable)

        val value = Value.from(expression.type, ValueSource.NONE)
        val constraint = flow.returnConstraint?.copy() ?: Constraint.from(value)

        // TODO: invocation side-effects
        // TODO: no-return invocations

        return MutableDataFrame(currentState.frame) to ConstrainedValue(value, constraint)
    }
}