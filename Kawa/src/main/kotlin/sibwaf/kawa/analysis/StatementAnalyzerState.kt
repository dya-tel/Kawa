package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import sibwaf.kawa.MethodFlow
import sibwaf.kawa.UnreachableFrame
import sibwaf.kawa.calculation.ValueCalculatorState
import sibwaf.kawa.calculation.conditions.ConditionCalculatorResult
import sibwaf.kawa.values.ConstrainedValue
import spoon.reflect.code.CtCFlowBreak
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtLocalVariable
import spoon.reflect.code.CtStatement
import spoon.reflect.reference.CtExecutableReference

data class StatementAnalyzerState(
        val annotation: MethodFlow,
        val frame: DataFrame,
        val localVariables: MutableSet<CtLocalVariable<*>>,
        val returnPoints: MutableSet<CtStatement>,
        val jumpPoints: MutableCollection<Pair<CtCFlowBreak, DataFrame>>,

        private val methodFlowProvider: suspend (CtExecutableReference<*>) -> MethodFlow,
        private val statementFlowProvider: suspend (StatementAnalyzerState, CtStatement) -> DataFrame,
        private val valueProvider: suspend (ValueCalculatorState, CtExpression<*>) -> Pair<DataFrame, ConstrainedValue>,
        private val conditionValueProvider: suspend (ValueCalculatorState, CtExpression<*>) -> ConditionCalculatorResult
) {
    fun toCalculatorState(): ValueCalculatorState {
        return ValueCalculatorState(
                annotation = annotation,
                frame = frame,
                methodFlowProvider = methodFlowProvider,
                valueProvider = valueProvider,
                conditionValueProvider = conditionValueProvider
        )
    }

    suspend fun getMethodFlow(executable: CtExecutableReference<*>): MethodFlow {
        return methodFlowProvider(executable)
    }

    suspend fun getStatementFlow(statement: CtStatement): DataFrame {
        annotation.frames[statement] = frame

        if (frame is UnreachableFrame) {
            return frame
        }

        return statementFlowProvider(this, statement)
    }

    suspend fun getValue(expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue> {
        return toCalculatorState().getValue(expression)
    }

    suspend fun getConditionValue(expression: CtExpression<*>): ConditionCalculatorResult {
        return toCalculatorState().getConditionValue(expression)
    }
}