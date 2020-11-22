package sibwaf.kawa.calculation.conditions

import sibwaf.kawa.calculation.ValueCalculatorState
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtUnaryOperator
import spoon.reflect.code.UnaryOperatorKind

class InvertedConditionCalculator : ConditionCalculator {

    override fun supports(expression: CtExpression<*>) = expression is CtUnaryOperator<*> && expression.kind == UnaryOperatorKind.NOT

    override suspend fun calculateCondition(state: ValueCalculatorState, expression: CtExpression<*>): ConditionCalculatorResult {
        expression as CtUnaryOperator<*>

        val (thenFrame, elseFrame, operandValue, operandConstraint) = state.getConditionValue(expression.operand)
        return ConditionCalculatorResult(
                thenFrame = elseFrame,
                elseFrame = thenFrame,
                value = operandValue.invert(),
                constraint = operandConstraint.invert()
        )
    }
}