package sibwaf.kawa.calculation

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.values.ConstrainedValue
import spoon.reflect.code.CtAssignment
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtVariableAccess
import spoon.reflect.declaration.CtField

class CtAssignmentCalculator : ValueCalculator {

    override fun supports(expression: CtExpression<*>) = expression is CtAssignment<*, *>

    override suspend fun calculate(state: AnalyzerState, expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue> {
        expression as CtAssignment<*, *>

        val (frame, result) = state.getValue(expression.assignment)

        val variable = (expression.assigned as? CtVariableAccess<*>)
            ?.variable
            ?.declaration
            ?.takeUnless { it is CtField<*> }

        val resultFrame = if (variable != null) {
            MutableDataFrame(frame).apply {
                setValue(variable, result.value)
                setConstraint(result.value, result.constraint)
            }
        } else {
            frame
        }

        return resultFrame to result
    }
}