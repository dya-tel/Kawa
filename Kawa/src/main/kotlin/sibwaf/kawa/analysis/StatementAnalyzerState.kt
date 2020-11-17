package sibwaf.kawa.analysis

import sibwaf.kawa.DataFrame
import sibwaf.kawa.MethodFlow
import sibwaf.kawa.ValueCalculator
import sibwaf.kawa.calculation.conditions.ConditionCalculatorResult
import sibwaf.kawa.values.ConstrainedValue
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtLocalVariable
import spoon.reflect.code.CtStatement
import spoon.reflect.reference.CtExecutableReference

data class StatementAnalyzerState(
        val annotation: MethodFlow,
        val frame: DataFrame,
        val localVariables: MutableSet<CtLocalVariable<*>>,
        val returnPoints: MutableSet<CtStatement>,

        private val methodFlowProvider: suspend (CtExecutableReference<*>) -> MethodFlow,
        private val statementFlowProvider: suspend (StatementAnalyzerState, CtStatement) -> DataFrame,
        private val valueProvider: suspend (StatementAnalyzerState, CtExpression<*>) -> ConstrainedValue
) {
    suspend fun getMethodFlow(executable: CtExecutableReference<*>): MethodFlow {
        return methodFlowProvider(executable)
    }

    suspend fun getStatementFlow(statement: CtStatement): DataFrame {
        return statementFlowProvider(this, statement)
    }

    suspend fun getValue(expression: CtExpression<*>): ConstrainedValue {
        return valueProvider(this, expression)
    }

    suspend fun getConditionValue(expression: CtExpression<*>): ConditionCalculatorResult {
        // TODO
        return ValueCalculator.calculateCondition(
                annotation,
                frame,
                expression,
                this::getMethodFlow
        )
    }
}