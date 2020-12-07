package sibwaf.kawa.calculation.conditions

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.ReachableFrame
import sibwaf.kawa.UnreachableFrame
import sibwaf.kawa.constraints.BooleanConstraint
import sibwaf.kawa.constraints.Constraint
import sibwaf.kawa.constraints.ReferenceConstraint
import sibwaf.kawa.values.BooleanValue
import sibwaf.kawa.values.Value
import sibwaf.kawa.values.ValueSource
import spoon.reflect.code.BinaryOperatorKind
import spoon.reflect.code.CtBinaryOperator
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtLiteral

private data class InferredConstraint(
    val value: Value,
    val thenConstraint: Constraint,
    val elseConstraint: Constraint
)

class EqualityConditionCalculator : ConditionCalculator {

    override fun supports(expression: CtExpression<*>) =
        expression is CtBinaryOperator<*> && (expression.kind == BinaryOperatorKind.EQ || expression.kind == BinaryOperatorKind.NE)

    private fun inferEqualityConstraint(operator: CtBinaryOperator<*>, leftValue: Value, rightValue: Value): InferredConstraint? {
        val leftOperand = operator.leftHandOperand
        val rightOperand = operator.rightHandOperand

        // TODO: equal values
        return if (leftOperand is CtLiteral<*> && leftOperand.value == null) {
            InferredConstraint(
                value = rightValue,
                thenConstraint = ReferenceConstraint.createNull(),
                elseConstraint = ReferenceConstraint.createNonNull()
            )
        } else if (rightOperand is CtLiteral<*> && rightOperand.value == null) {
            InferredConstraint(
                value = leftValue,
                thenConstraint = ReferenceConstraint.createNull(),
                elseConstraint = ReferenceConstraint.createNonNull()
            )
        } else {
            null
        }
    }

    override suspend fun calculateCondition(state: AnalyzerState, expression: CtExpression<*>): ConditionCalculatorResult {
        expression as CtBinaryOperator<*>

        val (leftFrame, leftValue) = state.getValue(expression.leftHandOperand)
        if (leftFrame !is ReachableFrame) {
            return ConditionCalculatorResult(leftFrame, leftFrame, BooleanValue(ValueSource.NONE), BooleanConstraint.createUnknown())
        }

        val (rightFrame, rightValue) = state.copy(frame = leftFrame).getValue(expression.rightHandOperand)
        if (rightFrame !is ReachableFrame) {
            return ConditionCalculatorResult(rightFrame, rightFrame, BooleanValue(ValueSource.NONE), BooleanConstraint.createUnknown())
        }

        val leftConstraint = leftValue.constraint
        val rightConstraint = rightValue.constraint

        val resultConstraint = when {
            leftValue.value isSameAs rightValue.value -> BooleanConstraint.createTrue()
            else -> leftConstraint.isEqual(rightConstraint)
        }

        val thenFrame = if (resultConstraint.isFalse) UnreachableFrame.after(rightFrame) else MutableDataFrame(rightFrame)
        val elseFrame = if (resultConstraint.isTrue) UnreachableFrame.after(rightFrame) else MutableDataFrame(rightFrame)

        inferEqualityConstraint(expression, leftValue.value, rightValue.value)?.let { (value, thenConstraint, elseConstraint) ->
            // TODO: looks kinda hack-ish, should probably rework
            if (thenFrame is MutableDataFrame) {
                thenFrame.setConstraint(value, thenConstraint)
            }
            if (elseFrame is MutableDataFrame) {
                elseFrame.setConstraint(value, elseConstraint)
            }
        }

        return if (expression.kind == BinaryOperatorKind.EQ) {
            ConditionCalculatorResult(
                thenFrame = thenFrame,
                elseFrame = elseFrame,
                value = BooleanValue(ValueSource.NONE),
                constraint = resultConstraint
            )
        } else {
            ConditionCalculatorResult(
                thenFrame = elseFrame,
                elseFrame = thenFrame,
                value = BooleanValue(ValueSource.NONE),
                constraint = resultConstraint.invert()
            )
        }
    }
}