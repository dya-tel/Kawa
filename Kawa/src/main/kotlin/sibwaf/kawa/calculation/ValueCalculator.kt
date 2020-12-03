package sibwaf.kawa.calculation

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
import sibwaf.kawa.values.ConstrainedValue
import spoon.reflect.code.CtExpression

interface ValueCalculator {
    fun supports(expression: CtExpression<*>): Boolean
    suspend fun calculate(state: AnalyzerState, expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue>
}