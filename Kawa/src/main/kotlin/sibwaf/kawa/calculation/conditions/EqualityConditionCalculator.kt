package sibwaf.kawa.calculation.conditions

import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.calculation.ValueCalculatorState
import sibwaf.kawa.constraints.BooleanConstraint
import sibwaf.kawa.constraints.Constraint
import sibwaf.kawa.constraints.Nullability
import sibwaf.kawa.constraints.ReferenceConstraint
import sibwaf.kawa.constraints.TRUE_CONSTRAINT
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
                    thenConstraint = ReferenceConstraint().apply { nullability = Nullability.ALWAYS_NULL },
                    elseConstraint = ReferenceConstraint().apply { nullability = Nullability.NEVER_NULL }
            )
            /*if (operator.kind == BinaryOperatorKind.EQ) {
                rightOperand to ReferenceConstraint().apply { nullability = Nullability.ALWAYS_NULL }
            } else {
                rightOperand to ReferenceConstraint().apply { nullability = Nullability.NEVER_NULL }
            }*/
        } else if (rightOperand is CtLiteral<*> && rightOperand.value == null) {
            InferredConstraint(
                    expression = leftOperand,
                    thenConstraint = ReferenceConstraint().apply { nullability = Nullability.ALWAYS_NULL },
                    elseConstraint = ReferenceConstraint().apply { nullability = Nullability.NEVER_NULL }
            )
            /*if (operator.kind == BinaryOperatorKind.EQ) {
                leftOperand to ReferenceConstraint().apply { nullability = Nullability.ALWAYS_NULL }
            } else {
                leftOperand to ReferenceConstraint().apply { nullability = Nullability.NEVER_NULL }
            }*/
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
            leftValue.value isSameAs rightValue.value -> TRUE_CONSTRAINT
            leftConstraint is BooleanConstraint && rightConstraint is BooleanConstraint -> leftConstraint.isEqual(rightConstraint)
            leftConstraint is ReferenceConstraint && rightConstraint is ReferenceConstraint -> leftConstraint.isEqual(rightConstraint)
            else -> BooleanConstraint()
        }

        val thenFrame = MutableDataFrame(operatorNextFrame).apply { isReachable = !resultConstraint.isFalse }
        val elseFrame = MutableDataFrame(operatorNextFrame).apply { isReachable = !resultConstraint.isTrue }

        inferEqualityConstraint(expression)?.let { (expression, thenConstraint, elseConstraint) ->
            val declaration = (expression as? CtVariableRead<*>)?.variable?.declaration
            if (declaration != null) {
                thenFrame.setConstraint(declaration, thenConstraint)
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