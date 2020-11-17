package sibwaf.kawa.calculation

import sibwaf.kawa.DataFrame
import sibwaf.kawa.MethodFlow
import sibwaf.kawa.calculation.conditions.ConditionCalculatorResult
import sibwaf.kawa.values.ConstrainedValue
import spoon.reflect.code.CtExpression
import spoon.reflect.reference.CtExecutableReference

data class ValueCalculatorState(
        val annotation: MethodFlow,
        val frame: DataFrame,

        private val methodFlowProvider: suspend (CtExecutableReference<*>) -> MethodFlow,
        private val valueProvider: suspend (ValueCalculatorState, CtExpression<*>) -> Pair<DataFrame, ConstrainedValue>,
        private val conditionValueProvider: suspend (ValueCalculatorState, CtExpression<*>) -> ConditionCalculatorResult
) {
    suspend fun getMethodFlow(executable: CtExecutableReference<*>): MethodFlow {
        return methodFlowProvider(executable)
    }

    suspend fun getValue(expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue> {
        return valueProvider(this, expression)
    }

    suspend fun getConditionValue(expression: CtExpression<*>): ConditionCalculatorResult {
        return conditionValueProvider(this, expression)
    }
}