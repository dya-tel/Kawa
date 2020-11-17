package sibwaf.kawa.calculation

import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.constraints.Nullability
import sibwaf.kawa.constraints.ReferenceConstraint
import sibwaf.kawa.values.ConstrainedValue
import sibwaf.kawa.values.Value
import sibwaf.kawa.values.ValueSource
import spoon.reflect.code.CtConstructorCall
import spoon.reflect.code.CtExpression

// TODO: merge with CtInvocationCalculator
class CtConstructorCallCalculator : ValueCalculator {

    override fun supports(expression: CtExpression<*>) = expression is CtConstructorCall<*>

    override suspend fun calculate(state: ValueCalculatorState, expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue> {
        expression as CtConstructorCall<*>

        var currentState = state
        for (argument in expression.arguments) {
            val (nextFrame, _) = currentState.getValue(argument)
            currentState = state.copy(frame = nextFrame)
        }

        val value = Value.from(expression, ValueSource.NONE)
        val constraint = ReferenceConstraint().apply { nullability = Nullability.NEVER_NULL }

        // TODO: constructor side-effects

        return MutableDataFrame(currentState.frame) to ConstrainedValue(value, constraint)
    }
}