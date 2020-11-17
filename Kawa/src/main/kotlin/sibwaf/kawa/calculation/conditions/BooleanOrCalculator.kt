package sibwaf.kawa.calculation.conditions

import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.calculation.ValueCalculatorState
import sibwaf.kawa.constraints.BooleanConstraint
import sibwaf.kawa.constraints.TRUE_CONSTRAINT
import sibwaf.kawa.utility.flattenExpression
import sibwaf.kawa.values.BooleanValue
import sibwaf.kawa.values.ConstrainedValue
import sibwaf.kawa.values.ValueSource
import spoon.reflect.code.BinaryOperatorKind
import spoon.reflect.code.CtBinaryOperator
import spoon.reflect.code.CtExpression

class BooleanOrCalculator : ConditionCalculator {

    override fun supports(expression: CtExpression<*>) = expression is CtBinaryOperator<*> && expression.kind == BinaryOperatorKind.OR

    override suspend fun calculateCondition(state: ValueCalculatorState, expression: CtExpression<*>): ConditionCalculatorResult {
        expression as CtBinaryOperator<*>

        val operands = flattenExpression(expression, BinaryOperatorKind.OR)

        val thenFrames = ArrayList<DataFrame>(operands.size)
        var elseFrame = state.frame

        val constraints = ArrayList<BooleanConstraint>(operands.size)
        var nextState = state
        for (operand in operands) {
            val (operandThenFrame, operandElseFrame, value) = nextState.getConditionValue(operand)

            constraints += value.constraint as? BooleanConstraint ?: BooleanConstraint()

            thenFrames += operandThenFrame.compact(state.frame)
            elseFrame = operandElseFrame

            nextState = state.copy(frame = operandElseFrame)
        }

        val thenFrame = DataFrame.merge(state.frame, thenFrames)

        var result = constraints.first()
        for ((index, constraint) in constraints.withIndex()) {
            result = result.or(constraint)

            if (result.isTrue || constraints.subList(0, index).any { constraint.and(it).isTrue }) {
                result = TRUE_CONSTRAINT
                break
            }
        }

        return ConditionCalculatorResult(
                thenFrame = MutableDataFrame(thenFrame).apply { isReachable = !result.isFalse },
                elseFrame = MutableDataFrame(elseFrame).apply { isReachable = !result.isTrue },
                value = ConstrainedValue(BooleanValue(ValueSource.NONE), result)
        )
    }
}
