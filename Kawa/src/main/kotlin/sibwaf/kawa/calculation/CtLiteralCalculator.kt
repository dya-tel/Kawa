package sibwaf.kawa.calculation

import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.constraints.Nullability
import sibwaf.kawa.constraints.ReferenceConstraint
import sibwaf.kawa.values.ConstrainedValue
import sibwaf.kawa.values.ReferenceValue
import sibwaf.kawa.values.ValueSource
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtLiteral

class CtLiteralCalculator : ValueCalculator {

    override fun supports(expression: CtExpression<*>) = expression is CtLiteral<*>

    override suspend fun calculate(state: ValueCalculatorState, expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue> {
        expression as CtLiteral<*>

        return MutableDataFrame(state.frame) to when (expression.value) {
            null -> ConstrainedValue(ReferenceValue(ValueSource.NONE), ReferenceConstraint().apply { nullability = Nullability.ALWAYS_NULL })
            else -> ConstrainedValue.from(expression, ValueSource.NONE)
        }
    }
}