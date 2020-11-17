package sibwaf.kawa.calculation

import sibwaf.kawa.DataFrame
import sibwaf.kawa.values.ConstrainedValue
import sibwaf.kawa.values.ValueSource
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtUnaryOperator

class CtUnaryOperatorCalculator : ValueCalculator {

    override fun supports(expression: CtExpression<*>) = expression is CtUnaryOperator<*>

    override suspend fun calculate(state: ValueCalculatorState, expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue> {
        expression as CtUnaryOperator<*>

        // TODO

        val (frame, operand) = state.getValue(expression.operand)

        val result = when (expression.kind) {
            else -> null
        }

        return frame to (result ?: ConstrainedValue.from(expression, ValueSource.NONE))
    }
}