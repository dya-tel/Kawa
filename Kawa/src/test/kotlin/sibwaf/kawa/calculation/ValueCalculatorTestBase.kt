package sibwaf.kawa.calculation

import sibwaf.kawa.DataFrame
import sibwaf.kawa.EmptyFlow
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.calculation.conditions.ConditionCalculator
import sibwaf.kawa.calculation.conditions.ConditionCalculatorResult
import sibwaf.kawa.values.ConstrainedValue
import spoon.reflect.code.CtExpression

abstract class ValueCalculatorTestBase {

    protected open class TestValueCalculator(
            private val calculators: List<ValueCalculator>,
            private val conditionCalculators: List<ConditionCalculator> = calculators.filterIsInstance<ConditionCalculator>()
    ) : ConditionCalculator {

        override fun supports(expression: CtExpression<*>) = true

        override suspend fun calculateCondition(state: ValueCalculatorState, expression: CtExpression<*>): ConditionCalculatorResult {
            for (calculator in conditionCalculators) {
                if (calculator.supports(expression)) {
                    return calculator.calculateCondition(state, expression)
                }
            }

            throw IllegalStateException("No condition calculator registered for ${expression.javaClass}")
        }

        override suspend fun calculate(state: ValueCalculatorState, expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue> {
            for (calculator in calculators) {
                if (calculator.supports(expression)) {
                    return calculator.calculate(state, expression)
                }
            }

            throw IllegalStateException("No calculator registered for ${expression.javaClass}")
        }
    }

    protected fun createState(calculator: ConditionCalculator): ValueCalculatorState {
        return ValueCalculatorState(
                annotation = EmptyFlow,
                frame = MutableDataFrame(null),
                methodFlowProvider = { EmptyFlow },
                valueProvider = { state, currentExpression -> calculator.calculate(state, currentExpression) },
                conditionValueProvider = { state, currentExpression -> calculator.calculateCondition(state, currentExpression) }
        )
    }

    /*protected suspend fun analyzeStatement(
            valueCalculator: ConditionCalculator,
            expression: CtExpression<*>,
            customizeState: ValueCalculatorState.() -> ValueCalculatorState = { this }
    ) {
        val state = ValueCalculatorState(
                frame = MutableDataFrame(null),
                methodFlowProvider = { EmptyFlow },
                valueProvider = { state, currentExpression -> valueCalculator.calculate(state, currentExpression) },
                conditionValueProvider = { state, currentExpression -> valueCalculator.calculateCondition(state, currentExpression) }
        )

        valueCalculator.calculateCondition(ex)
    }*/
}