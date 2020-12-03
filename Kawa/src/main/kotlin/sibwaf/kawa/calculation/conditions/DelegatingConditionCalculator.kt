package sibwaf.kawa.calculation.conditions

import sibwaf.kawa.AnalyzerState
import spoon.reflect.code.CtExpression

class DelegatingConditionCalculator(private val calculators: List<ConditionCalculator>) : ConditionCalculator {

    override fun supports(expression: CtExpression<*>): Boolean {
        return calculators.isNotEmpty() && calculators.any { it.supports(expression) }
    }

    override suspend fun calculateCondition(state: AnalyzerState, expression: CtExpression<*>): ConditionCalculatorResult {
        for (calculator in calculators) {
            if (calculator.supports(expression)) {
                return calculator.calculateCondition(state, expression)
            }
        }

        throw IllegalArgumentException("Unsupported expression: $expression")
    }
}