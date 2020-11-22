package sibwaf.kawa.calculation.conditions

import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.calculation.ValueCalculatorState
import sibwaf.kawa.constraints.BooleanConstraint
import sibwaf.kawa.constraints.FALSE_CONSTRAINT
import sibwaf.kawa.constraints.Nullability
import sibwaf.kawa.constraints.ReferenceConstraint
import sibwaf.kawa.values.BooleanValue
import sibwaf.kawa.values.ValueSource
import spoon.reflect.code.BinaryOperatorKind
import spoon.reflect.code.CtBinaryOperator
import spoon.reflect.code.CtExpression

class InstanceOfCalculator : ConditionCalculator {

    override fun supports(expression: CtExpression<*>) =
            expression is CtBinaryOperator<*> && expression.kind == BinaryOperatorKind.INSTANCEOF

    override suspend fun calculateCondition(state: ValueCalculatorState, expression: CtExpression<*>): ConditionCalculatorResult {
        expression as CtBinaryOperator<*>

        val thenFrame = MutableDataFrame(state.frame)
        val elseFrame = MutableDataFrame(state.frame)

        // TODO: add type constraints
        val (value, constraint) = state.getValue(expression.leftHandOperand).second
        thenFrame.setConstraint(value, ReferenceConstraint().apply { nullability = Nullability.NEVER_NULL })

        val resultConstraint = if ((constraint as? ReferenceConstraint)?.nullability == Nullability.ALWAYS_NULL) {
            FALSE_CONSTRAINT
        } else {
            BooleanConstraint()
        }

        return ConditionCalculatorResult(
                thenFrame = thenFrame,
                elseFrame = elseFrame,
                value = BooleanValue(ValueSource.NONE),
                constraint = resultConstraint
        )
    }
}