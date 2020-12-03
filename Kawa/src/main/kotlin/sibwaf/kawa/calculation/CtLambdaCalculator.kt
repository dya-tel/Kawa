package sibwaf.kawa.calculation

import sibwaf.kawa.AnalyzerState
import sibwaf.kawa.DataFrame
import sibwaf.kawa.MutableDataFrame
import sibwaf.kawa.constraints.ReferenceConstraint
import sibwaf.kawa.values.ConstrainedValue
import sibwaf.kawa.values.ReferenceValue
import sibwaf.kawa.values.ValueSource
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtLambda

class CtLambdaCalculator : ValueCalculator {

    override fun supports(expression: CtExpression<*>) = expression is CtLambda<*>

    override suspend fun calculate(state: AnalyzerState, expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue> {
        // TODO
        val value = ConstrainedValue(
            ReferenceValue(ValueSource.NONE),
            ReferenceConstraint.createNonNull()
        )

        return MutableDataFrame(state.frame) to value
    }
}