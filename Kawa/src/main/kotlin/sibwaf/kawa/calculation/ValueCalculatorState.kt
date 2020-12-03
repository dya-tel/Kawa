package sibwaf.kawa.calculation

import sibwaf.kawa.DataFrame
import sibwaf.kawa.MethodFlow
import sibwaf.kawa.UnreachableFrame
import sibwaf.kawa.calculation.conditions.ConditionCalculatorResult
import sibwaf.kawa.constraints.BooleanConstraint
import sibwaf.kawa.values.BooleanValue
import sibwaf.kawa.values.ConstrainedValue
import sibwaf.kawa.values.ValueSource
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
        annotation.frames[expression] = frame

        if (frame is UnreachableFrame) {
            return frame to ConstrainedValue.from(expression, ValueSource.NONE) // TODO: invalid value
        }

        return valueProvider(this, expression)
    }

    suspend fun getConditionValue(expression: CtExpression<*>): ConditionCalculatorResult {
        annotation.frames[expression] = frame

        if (frame is UnreachableFrame) {
            // TODO: invalid result
            return ConditionCalculatorResult(
                    thenFrame = frame,
                    elseFrame = frame,
                    value = BooleanValue(ValueSource.NONE),
                    constraint = BooleanConstraint.createUnknown()
            )
        }

        return conditionValueProvider(this, expression)
    }
}