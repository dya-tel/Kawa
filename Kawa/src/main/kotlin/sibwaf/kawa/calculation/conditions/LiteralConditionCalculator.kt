package sibwaf.kawa.calculation.conditions

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.UnreachableFrame
import sibwaf.kawa.constraints.BooleanConstraint
import sibwaf.kawa.values.BooleanValue
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtLiteral

class LiteralConditionCalculator : ConditionCalculator {

    override fun supports(expression: CtExpression<*>) =
        expression is CtLiteral<*> && (expression.value == true || expression.value == false)

    override suspend fun calculateCondition(state: AnalyzerState, expression: CtExpression<*>): ConditionCalculatorResult {
        val value = (expression as CtLiteral<*>).value as Boolean

        val thenFrame = MutableDataFrame(state.frame)
        val elseFrame = MutableDataFrame(state.frame)

        val constraint = if (value) {
            BooleanConstraint.createTrue()
        } else {
            BooleanConstraint.createFalse()
        }

        return ConditionCalculatorResult(
            thenFrame = if (constraint.isFalse) UnreachableFrame.after(state.frame) else thenFrame,
            elseFrame = if (constraint.isTrue) UnreachableFrame.after(state.frame) else elseFrame,
            value = BooleanValue(expression),
            constraint = constraint
        )
    }
}