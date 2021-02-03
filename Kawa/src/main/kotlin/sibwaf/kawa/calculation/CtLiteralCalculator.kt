package sibwaf.kawa.calculation

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.constraints.ReferenceConstraint
import sibwaf.kawa.values.ConstrainedValue
import sibwaf.kawa.values.ReferenceValue
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtLiteral

class CtLiteralCalculator : ValueCalculator {

    override fun supports(expression: CtExpression<*>) = expression is CtLiteral<*>

    override suspend fun calculate(state: AnalyzerState, expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue> {
        expression as CtLiteral<*>

        return MutableDataFrame(state.frame) to when (expression.value) {
            null -> ConstrainedValue(ReferenceValue(expression), ReferenceConstraint.createNull())
            else -> ConstrainedValue.from(expression)
        }
    }
}