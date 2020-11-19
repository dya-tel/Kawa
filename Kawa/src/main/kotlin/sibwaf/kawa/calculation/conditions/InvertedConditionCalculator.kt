package sibwaf.kawa.calculation.conditions

import sibwaf.kawa.calculation.ValueCalculatorState
import sibwaf.kawa.constraints.BooleanConstraint
import sibwaf.kawa.values.BooleanValue
import sibwaf.kawa.values.ConstrainedValue
import sibwaf.kawa.values.InvertedBooleanValue
import sibwaf.kawa.values.ValueSource
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtUnaryOperator
import spoon.reflect.code.UnaryOperatorKind

class InvertedConditionCalculator : ConditionCalculator {

    override fun supports(expression: CtExpression<*>) = expression is CtUnaryOperator<*> && expression.kind == UnaryOperatorKind.NOT

    override suspend fun calculateCondition(state: ValueCalculatorState, expression: CtExpression<*>): ConditionCalculatorResult {
        expression as CtUnaryOperator<*>

        val (thenFrame, elseFrame, operand) = state.getConditionValue(expression.operand)
        return ConditionCalculatorResult(
                thenFrame = elseFrame,
                elseFrame = thenFrame,
                value = ConstrainedValue(
                        (operand.value as? BooleanValue)?.invert() ?: BooleanValue(ValueSource.NONE),
                        (operand.constraint as? BooleanConstraint)?.invert() ?: BooleanConstraint()
                )
        )
    }
}