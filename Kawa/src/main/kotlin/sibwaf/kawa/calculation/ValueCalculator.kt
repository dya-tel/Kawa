package sibwaf.kawa.calculation

import sibwaf.kawa.DataFrame
import sibwaf.kawa.values.ConstrainedValue
import spoon.reflect.code.CtExpression

interface ValueCalculator {
    fun supports(expression: CtExpression<*>): Boolean
    suspend fun calculate(state: ValueCalculatorState, expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue>
}