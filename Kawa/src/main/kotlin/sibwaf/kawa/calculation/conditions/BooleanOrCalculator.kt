package sibwaf.kawa.calculation.conditions

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
import sibwaf.kawa.ReachableFrame
import sibwaf.kawa.UnreachableFrame
import sibwaf.kawa.constraints.BooleanConstraint
import sibwaf.kawa.utility.flattenExpression
import sibwaf.kawa.values.BooleanValue
import sibwaf.kawa.values.ValueSource
import spoon.reflect.code.BinaryOperatorKind
import spoon.reflect.code.CtBinaryOperator
import spoon.reflect.code.CtExpression

class BooleanOrCalculator : ConditionCalculator {

    override fun supports(expression: CtExpression<*>) = expression is CtBinaryOperator<*> && expression.kind == BinaryOperatorKind.OR

    override suspend fun calculateCondition(state: AnalyzerState, expression: CtExpression<*>): ConditionCalculatorResult {
        expression as CtBinaryOperator<*>

        val operands = flattenExpression(expression, BinaryOperatorKind.OR)

        val thenFrames = ArrayList<DataFrame>(operands.size)
        var elseFrame: DataFrame = state.frame

        val constraints = ArrayList<BooleanConstraint>(operands.size)
        var nextState = state
        for (operand in operands) {
            val (operandThenFrame, operandElseFrame, _, operandConstraint) = nextState.getConditionValue(operand)
            constraints += operandConstraint

            thenFrames += operandThenFrame.compact(state.frame)
            elseFrame = operandElseFrame

            if (operandElseFrame !is ReachableFrame) {
                break
            }
            nextState = state.copy(frame = operandElseFrame)
        }

        val thenFrame = DataFrame.merge(state.frame, thenFrames)

        var result = constraints.first()
        for ((index, constraint) in constraints.withIndex()) {
            result = result.or(constraint)

            if (result.isTrue || constraints.subList(0, index).any { constraint.and(it).isTrue }) {
                result = BooleanConstraint.createTrue()
                break
            }
        }

        return ConditionCalculatorResult(
            thenFrame = if (result.isFalse) UnreachableFrame.after(thenFrame) else thenFrame,
            elseFrame = if (result.isTrue) UnreachableFrame.after(elseFrame) else elseFrame,
            value = BooleanValue(ValueSource.NONE),
            constraint = result
        )
    }
}
