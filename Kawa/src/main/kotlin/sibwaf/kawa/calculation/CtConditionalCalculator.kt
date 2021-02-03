package sibwaf.kawa.calculation

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
import sibwaf.kawa.ReachableFrame
import sibwaf.kawa.constraints.Constraint
import sibwaf.kawa.values.ConstrainedValue
import sibwaf.kawa.values.Value
import spoon.reflect.code.CtConditional
import spoon.reflect.code.CtExpression
import java.util.LinkedList

class CtConditionalCalculator : ValueCalculator {

    override fun supports(expression: CtExpression<*>) = expression is CtConditional<*>

    override suspend fun calculate(state: AnalyzerState, expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue> {
        expression as CtConditional<*>

        var (thenFrame, elseFrame, _) = state.getConditionValue(expression.condition)

        val values = LinkedList<ConstrainedValue>()

        if (thenFrame is ReachableFrame) {
            val (frame, value) = state.copy(frame = thenFrame).getValue(expression.thenExpression)
            thenFrame = frame
            values += value
        }

        if (elseFrame is ReachableFrame) {
            val (frame, value) = state.copy(frame = elseFrame).getValue(expression.elseExpression)
            elseFrame = frame
            values += value
        }

        val resultFrame = DataFrame.merge(
            state.frame,
            thenFrame.compact(state.frame),
            elseFrame.compact(state.frame)
        )

        // TODO: composite value
        val resultValue = Value.from(expression)
        val resultConstraint = values.map { it.constraint }.reduce(Constraint::merge)

        return resultFrame to ConstrainedValue(resultValue, resultConstraint)
    }
}