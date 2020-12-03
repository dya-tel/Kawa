package sibwaf.kawa.calculation

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
import sibwaf.kawa.values.ConstrainedValue
import sibwaf.kawa.values.Value
import sibwaf.kawa.values.ValueSource
import spoon.reflect.code.CtConditional
import spoon.reflect.code.CtExpression

class CtConditionalCalculator : ValueCalculator {

    override fun supports(expression: CtExpression<*>) = expression is CtConditional<*>

    override suspend fun calculate(state: AnalyzerState, expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue> {
        expression as CtConditional<*>

        val (thenFrame, elseFrame, _) = state.getConditionValue(expression.condition)

        val (thenFrame2, thenValue) = state.copy(frame = thenFrame).getValue(expression.thenExpression)
        val (elseFrame2, elseValue) = state.copy(frame = elseFrame).getValue(expression.elseExpression)

        val resultFrame = DataFrame.merge(
                state.frame,
                thenFrame2.compact(state.frame),
                elseFrame2.compact(state.frame)
        )

        val resultValue = Value.from(expression, ValueSource.NONE)
        val resultConstraint = thenValue.constraint.merge(elseValue.constraint)

        return resultFrame to ConstrainedValue(resultValue, resultConstraint)
    }
}