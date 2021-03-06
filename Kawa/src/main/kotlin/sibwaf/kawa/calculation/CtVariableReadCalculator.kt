package sibwaf.kawa.calculation

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.values.ConstrainedValue
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtVariableRead
import spoon.reflect.reference.CtFieldReference

class CtVariableReadCalculator : ValueCalculator {

    override fun supports(expression: CtExpression<*>) = expression is CtVariableRead<*>

    override suspend fun calculate(state: AnalyzerState, expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue> {
        expression as CtVariableRead<*>

        val value = expression
            .variable
            .takeUnless { it is CtFieldReference<*> }
            ?.declaration
            ?.let { state.frame.getValue(it) }

        val constraint = value?.let { state.frame.getConstraint(it) }

        val result = if (value != null && constraint != null) {
            ConstrainedValue(value, constraint)
        } else {
            ConstrainedValue.from(expression)
        }

        return MutableDataFrame(state.frame) to result
    }
}