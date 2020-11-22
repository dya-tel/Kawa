package sibwaf.kawa.calculation.conditions

import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.calculation.ValueCalculatorState
import sibwaf.kawa.constraints.BooleanConstraint
import sibwaf.kawa.constraints.FALSE_CONSTRAINT
import sibwaf.kawa.constraints.TRUE_CONSTRAINT
import sibwaf.kawa.values.BooleanValue
import sibwaf.kawa.values.ValueSource
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtVariableRead

class VariableReadConditionCalculator : ConditionCalculator {

    override fun supports(expression: CtExpression<*>) = expression is CtVariableRead<*>

    override suspend fun calculateCondition(state: ValueCalculatorState, expression: CtExpression<*>): ConditionCalculatorResult {
        val thenFrame = MutableDataFrame(state.frame)
        val elseFrame = MutableDataFrame(state.frame)

        val declaration = (expression as CtVariableRead<*>).variable?.declaration
        if (declaration != null) {
            thenFrame.setConstraint(declaration, TRUE_CONSTRAINT)
            elseFrame.setConstraint(declaration, FALSE_CONSTRAINT)
        }

        val value = declaration?.let { state.frame.getValue(it) as? BooleanValue } ?: BooleanValue(ValueSource.NONE)
        val constraint = (declaration?.let { state.frame.getConstraint(it) } as? BooleanConstraint) ?: BooleanConstraint()

        return ConditionCalculatorResult(
                thenFrame = thenFrame.apply { isReachable = !constraint.isFalse },
                elseFrame = elseFrame.apply { isReachable = !constraint.isTrue },
                value = value,
                constraint = constraint
        )
    }
}