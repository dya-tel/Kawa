package sibwaf.kawa

import sibwaf.kawa.calculation.conditions.ConditionCalculatorResult
import sibwaf.kawa.values.ConstrainedValue
import spoon.reflect.code.CtCFlowBreak
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtLocalVariable
import spoon.reflect.code.CtStatement
import spoon.reflect.reference.CtExecutableReference

data class AnalyzerState(
    val annotation: MethodFlow,
    val frame: ReachableFrame,
    val localVariables: MutableSet<CtLocalVariable<*>>,
    val jumpPoints: MutableCollection<Pair<CtCFlowBreak, ReachableFrame>>,

    private val methodFlowProvider: suspend (CtExecutableReference<*>) -> MethodFlow,
    private val statementFlowProvider: suspend (AnalyzerState, CtStatement) -> DataFrame,
    private val valueProvider: suspend (AnalyzerState, CtExpression<*>) -> Pair<DataFrame, ConstrainedValue>,
    private val conditionValueProvider: suspend (AnalyzerState, CtExpression<*>) -> ConditionCalculatorResult
) {

    suspend fun getMethodFlow(executable: CtExecutableReference<*>): MethodFlow {
        return methodFlowProvider(executable)
    }

    suspend fun getStatementFlow(statement: CtStatement): DataFrame {
        annotation.frames[statement] = frame
        return statementFlowProvider(this, statement)
    }

    suspend fun getValue(expression: CtExpression<*>): Pair<DataFrame, ConstrainedValue> {
        annotation.frames[expression] = frame
        return valueProvider(this, expression)
    }

    suspend fun getConditionValue(expression: CtExpression<*>): ConditionCalculatorResult {
        annotation.frames[expression] = frame
        return conditionValueProvider(this, expression)
    }
}