package sibwaf.kawa.calculation

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
import sibwaf.kawa.values.ConstrainedValue
import spoon.reflect.code.CtExpression

class DelegatingValueCalculator(private val calculators: List<ValueCalculator>) : ValueCalculator {

    override fun supports(expression: CtExpression<*>): Boolean {
        return calculators.isNotEmpty() && calculators.any { it.supports(expression) }
    }

    override suspend fun calculate(state: AnalyzerState, expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue> {
        for (calculator in calculators) {
            if (calculator.supports(expression)) {
                return calculator.calculate(state, expression)
            }
        }

        throw IllegalArgumentException("Unsupported expression: $expression")
    }
}