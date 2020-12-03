package sibwaf.kawa.calculation

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.UnreachableFrame
import sibwaf.kawa.constraints.Nullability
import sibwaf.kawa.constraints.ReferenceConstraint
import sibwaf.kawa.values.ConstrainedValue
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtTargetedExpression

abstract class CtTargetedExpressionCalculator : ValueCalculator {

    protected abstract suspend fun calculate(
        state: AnalyzerState,
        expression: CtExpression<*>,
        target: ConstrainedValue
    ): Pair<DataFrame, ConstrainedValue>

    final override suspend fun calculate(state: AnalyzerState, expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue> {
        expression as CtTargetedExpression<*, *>

        val (targetFrame, targetValue) = state.getValue(expression.target)

        val nullability = (targetValue.constraint as? ReferenceConstraint)?.nullability
        val frame = if (nullability == Nullability.ALWAYS_NULL) {
            UnreachableFrame.after(state.frame)
        } else {
            MutableDataFrame(targetFrame).apply {
                setConstraint(targetValue.value, ReferenceConstraint.createNonNull())
            }
        }

        return calculate(state.copy(frame = frame), expression, targetValue)
    }
}