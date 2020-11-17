package sibwaf.kawa.calculation

import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.values.ConstrainedValue
import sibwaf.kawa.values.ValueSource
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtVariableRead

class CtVariableReadCalculator : ValueCalculator {

    override fun supports(expression: CtExpression<*>) = expression is CtVariableRead<*>

    override suspend fun calculate(state: ValueCalculatorState, expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue> {
        expression as CtVariableRead<*>

        val value = expression.variable.declaration?.let { state.frame.getValue(it) }
        val constraint = value?.let { state.frame.getConstraint(it) }

        val result = if (value != null && constraint != null) {
            ConstrainedValue(value, constraint)
        } else {
            ConstrainedValue.from(expression, ValueSource.NONE)
        }

        return MutableDataFrame(state.frame) to result
    }
}