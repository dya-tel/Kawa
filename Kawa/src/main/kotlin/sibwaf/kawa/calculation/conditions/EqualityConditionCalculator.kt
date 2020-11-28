package sibwaf.kawa.calculation.conditions

import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.UnreachableFrame
import sibwaf.kawa.calculation.ValueCalculatorState
import sibwaf.kawa.constraints.BooleanConstraint
import sibwaf.kawa.constraints.Constraint
import sibwaf.kawa.constraints.ReferenceConstraint
import sibwaf.kawa.values.BooleanValue
import sibwaf.kawa.values.ValueSource
import spoon.reflect.code.BinaryOperatorKind
import spoon.reflect.code.CtBinaryOperator
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtLiteral
import spoon.reflect.code.CtVariableRead

private data class InferredConstraint(
        val expression: CtExpression<*>,
        val thenConstraint: Constraint,
        val elseConstraint: Constraint
)

class EqualityConditionCalculator : ConditionCalculator {

    override fun supports(expression: CtExpression<*>) =
            expression is CtBinaryOperator<*> && (expression.kind == BinaryOperatorKind.EQ || expression.kind == BinaryOperatorKind.NE)

    private fun inferEqualityConstraint(operator: CtBinaryOperator<*>): InferredConstraint? {
        val leftOperand = operator.leftHandOperand
        val rightOperand = operator.rightHandOperand

        return if (leftOperand is CtLiteral<*> && leftOperand.value == null) {
            InferredConstraint(
                    expression = rightOperand,
                    thenConstraint = ReferenceConstraint.createNull(),
                    elseConstraint = ReferenceConstraint.createNonNull()
            )
        } else if (rightOperand is CtLiteral<*> && rightOperand.value == null) {
            InferredConstraint(
                    expression = leftOperand,
                    thenConstraint = ReferenceConstraint.createNull(),
                    elseConstraint = ReferenceConstraint.createNonNull()
            )
        } else {
            null
        }
    }

    override suspend fun calculateCondition(state: ValueCalculatorState, expression: CtExpression<*>): ConditionCalculatorResult {
        expression as CtBinaryOperator<*>

        val (leftFrame, leftValue) = state.getValue(expression.leftHandOperand)
        val (rightFrame, rightValue) = state.getValue(expression.rightHandOperand)

        val operatorNextFrame = DataFrame.merge(
                state.frame,
                leftFrame.compact(state.frame),
                rightFrame.compact(state.frame)
        )

        val leftConstraint = leftValue.constraint
        val rightConstraint = rightValue.constraint

        val resultConstraint = when {
            leftValue.value isSameAs rightValue.value -> BooleanConstraint.createTrue()
            else -> leftConstraint.isEqual(rightConstraint)
        }

        val thenFrame = if (resultConstraint.isFalse) UnreachableFrame.after(operatorNextFrame) else MutableDataFrame(operatorNextFrame)
        val elseFrame = if (resultConstraint.isTrue) UnreachableFrame.after(operatorNextFrame) else MutableDataFrame(operatorNextFrame)

        inferEqualityConstraint(expression)?.let { (expression, thenConstraint, elseConstraint) ->
            val declaration = (expression as? CtVariableRead<*>)?.variable?.declaration ?: return@let

            // TODO: looks kinda hack-ish, should probably rework
            if (thenFrame is MutableDataFrame) {
                thenFrame.setConstraint(declaration, thenConstraint)
            }
            if (elseFrame is MutableDataFrame) {
                elseFrame.setConstraint(declaration, elseConstraint)
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