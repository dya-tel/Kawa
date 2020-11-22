package sibwaf.kawa.calculation.conditions

import sibwaf.kawa.DataFrame
import sibwaf.kawa.calculation.ValueCalculator
import sibwaf.kawa.calculation.ValueCalculatorState
import sibwaf.kawa.constraints.BooleanConstraint
import sibwaf.kawa.values.BooleanValue
import sibwaf.kawa.values.ConstrainedValue
import spoon.reflect.code.CtExpression

data class ConditionCalculatorResult(
        val thenFrame: DataFrame,
        val elseFrame: DataFrame,
        val value: BooleanValue,
        val constraint: BooleanConstraint
)

interface ConditionCalculator : ValueCalculator {

    suspend fun calculateCondition(state: ValueCalculatorState, expression: CtExpression<*>): ConditionCalculatorResult

    override suspend fun calculate(state: ValueCalculatorState, expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue> {
        val (thenFrame, elseFrame, value, constraint) = calculateCondition(state, expression)

        // TODO: is it needed?
        val resultFrame = DataFrame.merge(
                state.frame,
                thenFrame.compact(state.frame),
                elseFrame.compact(state.frame)
        )

        return resultFrame to ConstrainedValue(value, constraint)
    }
}