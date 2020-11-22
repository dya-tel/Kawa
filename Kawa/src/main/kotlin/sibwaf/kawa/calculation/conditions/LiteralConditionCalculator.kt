package sibwaf.kawa.calculation.conditions

import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.calculation.ValueCalculatorState
import sibwaf.kawa.constraints.FALSE_CONSTRAINT
import sibwaf.kawa.constraints.TRUE_CONSTRAINT
import sibwaf.kawa.values.BooleanValue
import sibwaf.kawa.values.ValueSource
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtLiteral

class LiteralConditionCalculator : ConditionCalculator {

    override fun supports(expression: CtExpression<*>) = expression is CtLiteral<*> && (expression.value == true || expression.value == false)

    override suspend fun calculateCondition(state: ValueCalculatorState, expression: CtExpression<*>): ConditionCalculatorResult {
        val value = (expression as CtLiteral<*>).value as Boolean

        val thenFrame = MutableDataFrame(state.frame)
        val elseFrame = MutableDataFrame(state.frame)

        val constraint = if (value) {
            TRUE_CONSTRAINT
        } else {
            FALSE_CONSTRAINT
        }

        return ConditionCalculatorResult(
                thenFrame = thenFrame.apply { isReachable = value },
                elseFrame = elseFrame.apply { isReachable = !value },
                value = BooleanValue(ValueSource.NONE),
                constraint = constraint
        )
    }
}