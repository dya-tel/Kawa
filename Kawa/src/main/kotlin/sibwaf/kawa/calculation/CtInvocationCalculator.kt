package sibwaf.kawa.calculation

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
import sibwaf.kawa.ReachableFrame
import sibwaf.kawa.values.ConstrainedValue
import sibwaf.kawa.values.ValueSource
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtInvocation
import java.util.LinkedList

class CtInvocationCalculator : CtTargetedExpressionCalculator() {

    override fun supports(expression: CtExpression<*>) = expression is CtInvocation<*>

    override suspend fun calculate(
        state: AnalyzerState,
        expression: CtExpression<*>,
        target: ConstrainedValue
    ): Pair<DataFrame, ConstrainedValue> {
        expression as CtInvocation<*>

        // TODO: proper invalid value
        fun invalidValue() = ConstrainedValue.from(expression, ValueSource.NONE)

        val arguments = LinkedList<ConstrainedValue>()

        var currentState = state
        for (argument in expression.arguments) {
            val (nextFrame, value) = currentState.getValue(argument)
            if (nextFrame !is ReachableFrame) {
                return nextFrame to invalidValue()
            }

            arguments += value
            currentState = currentState.copy(frame = nextFrame)
        }

        // TODO: copy state with a target
        // TODO: find proper executable by actual target type
        val (frame, value) = currentState.getInvocationFlow(expression.executable, arguments)
        return frame to (value ?: invalidValue())
    }
}