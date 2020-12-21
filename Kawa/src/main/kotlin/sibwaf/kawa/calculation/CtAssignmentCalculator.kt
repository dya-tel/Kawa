package sibwaf.kawa.calculation

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.ReachableFrame
import sibwaf.kawa.values.ConstrainedValue
import spoon.reflect.code.CtAssignment
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtVariableAccess
import spoon.reflect.reference.CtFieldReference

class CtAssignmentCalculator : ValueCalculator {

    override fun supports(expression: CtExpression<*>) = expression is CtAssignment<*, *>

    override suspend fun calculate(state: AnalyzerState, expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue> {
        expression as CtAssignment<*, *>

        val (frame, result) = state.getValue(expression.assignment)
        // TODO: exit on UnreachableFrame with invalid value

        val variable = (expression.assigned as? CtVariableAccess<*>)
            ?.variable
            ?.takeUnless { it is CtFieldReference<*> }
            ?.declaration

        val resultFrame = if (variable != null && frame is ReachableFrame) {
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