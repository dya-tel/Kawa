package sibwaf.kawa.calculation.conditions

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.UnreachableFrame
import sibwaf.kawa.constraints.BooleanConstraint
import sibwaf.kawa.values.BooleanValue
import sibwaf.kawa.values.ValueSource
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtVariableRead

class VariableReadConditionCalculator : ConditionCalculator {

    override fun supports(expression: CtExpression<*>) = expression is CtVariableRead<*>

    override suspend fun calculateCondition(state: AnalyzerState, expression: CtExpression<*>): ConditionCalculatorResult {
        val thenFrame = MutableDataFrame(state.frame)
        val elseFrame = MutableDataFrame(state.frame)

        val declaration = (expression as CtVariableRead<*>).variable?.declaration
        if (declaration != null) {
            thenFrame.setConstraint(declaration, BooleanConstraint.createTrue())
            elseFrame.setConstraint(declaration, BooleanConstraint.createFalse())
        }

        val value = declaration?.let { state.frame.getValue(it) as? BooleanValue } ?: BooleanValue(ValueSource.NONE)
        val constraint = (declaration?.let { state.frame.getConstraint(it) } as? BooleanConstraint) ?: BooleanConstraint.createUnknown()

        return ConditionCalculatorResult(
            thenFrame = if (constraint.isFalse) UnreachableFrame.after(state.frame) else thenFrame,
            elseFrame = if (constraint.isTrue) UnreachableFrame.after(state.frame) else elseFrame,
            value = value,
            constraint = constraint
        )
    }
}